package com.example.demo.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.TodoAttachment;

@Service
public class FileStorageService {

	private static final int MAX_FILE_SIZE = 10 * 1024 * 1024;
	private static final byte[] PNG_SIGNATURE = {
			(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A
	};

	private final Path storageRoot;
	private final VirusScanner virusScanner;

	public FileStorageService(
			@Value("${app.file-storage-dir:./uploads}") String storageDirectory,
			VirusScanner virusScanner) {
		this.storageRoot = Path.of(storageDirectory).toAbsolutePath().normalize();
		this.virusScanner = virusScanner;
	}

	public TodoAttachment store(Long todoId, MultipartFile upload) throws IOException {
		if (upload == null || upload.isEmpty()) {
			throw new AttachmentRejectedException("空のファイルは添付できません");
		}
		if (upload.getSize() > MAX_FILE_SIZE) {
			throw new AttachmentRejectedException("ファイルは10MB以下にしてください");
		}

		String originalFilename = sanitizeFilename(upload.getOriginalFilename());
		String extension = getAllowedExtension(originalFilename);
		byte[] content = upload.getBytes();
		if (content.length == 0 || content.length > MAX_FILE_SIZE) {
			throw new AttachmentRejectedException("ファイルサイズが許可範囲外です");
		}
		String contentType = validateAndGetContentType(extension, content);
		virusScanner.scan(content);

		Files.createDirectories(storageRoot);
		String storedFilename = UUID.randomUUID() + extension;
		Path destination = resolveStoredFile(storedFilename);
		try {
			Files.write(destination, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
		} catch (IOException exception) {
			try {
				Files.deleteIfExists(destination);
			} catch (IOException cleanupException) {
				exception.addSuppressed(cleanupException);
			}
			throw exception;
		}

		TodoAttachment attachment = new TodoAttachment();
		attachment.setTodoId(todoId);
		attachment.setStoredFilename(storedFilename);
		attachment.setOriginalFilename(originalFilename);
		attachment.setContentType(contentType);
		attachment.setFileSize(content.length);
		return attachment;
	}

	public Resource loadAsResource(String storedFilename) throws IOException {
		Path file = resolveStoredFile(storedFilename);
		if (Files.isSymbolicLink(file) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
			throw new NoSuchFileException(storedFilename);
		}
		Resource resource = new UrlResource(file.toUri());
		if (!resource.exists() || !resource.isReadable()) {
			throw new NoSuchFileException(storedFilename);
		}
		return resource;
	}

	public void deleteStoredFile(String storedFilename) throws IOException {
		Files.deleteIfExists(resolveStoredFile(storedFilename));
	}

	private Path resolveStoredFile(String storedFilename) {
		int extensionSeparator = storedFilename == null ? -1 : storedFilename.lastIndexOf('.');
		if (extensionSeparator <= 0) {
			throw new AttachmentRejectedException("添付ファイルの保存名が不正です");
		}
		String uuid = storedFilename.substring(0, extensionSeparator);
		String extension = storedFilename.substring(extensionSeparator).toLowerCase(Locale.ROOT);
		try {
			if (!UUID.fromString(uuid).toString().equals(uuid)
					|| !isAllowedExtension(extension)) {
				throw new AttachmentRejectedException("添付ファイルの保存名が不正です");
			}
		} catch (IllegalArgumentException exception) {
			throw new AttachmentRejectedException("添付ファイルの保存名が不正です");
		}
		Path file = storageRoot.resolve(storedFilename).normalize();
		if (!file.startsWith(storageRoot)) {
			throw new AttachmentRejectedException("添付ファイルの保存先が不正です");
		}
		return file;
	}

	private static String sanitizeFilename(String submittedFilename) {
		if (submittedFilename == null || submittedFilename.isBlank()) {
			throw new AttachmentRejectedException("ファイル名を取得できません");
		}
		String normalizedPath = submittedFilename.replace('\\', '/');
		String filename = normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1);
		filename = Normalizer.normalize(filename, Normalizer.Form.NFC)
				.replaceAll("[\\p{Cntrl}]", "_")
				.trim()
				.replaceAll("[. ]+$", "");
		if (filename.isBlank() || filename.equals(".") || filename.equals("..")) {
			throw new AttachmentRejectedException("ファイル名が不正です");
		}
		if (filename.codePointCount(0, filename.length()) > 255) {
			filename = filename.substring(0, filename.offsetByCodePoints(0, 255));
		}
		return filename;
	}

	private static String getAllowedExtension(String filename) {
		int extensionSeparator = filename.lastIndexOf('.');
		String extension = extensionSeparator < 0
				? ""
				: filename.substring(extensionSeparator).toLowerCase(Locale.ROOT);
		if (!isAllowedExtension(extension)) {
			throw new AttachmentRejectedException("許可されている形式はPDF、JPEG、PNG、TXTです");
		}
		return extension;
	}

	private static boolean isAllowedExtension(String extension) {
		return extension.equals(".pdf")
				|| extension.equals(".jpg")
				|| extension.equals(".jpeg")
				|| extension.equals(".png")
				|| extension.equals(".txt");
	}

	private static String validateAndGetContentType(String extension, byte[] content) {
		boolean valid = switch (extension) {
			case ".pdf" -> startsWith(content, "%PDF-".getBytes(StandardCharsets.US_ASCII));
			case ".jpg", ".jpeg" -> content.length >= 3
					&& (content[0] & 0xFF) == 0xFF
					&& (content[1] & 0xFF) == 0xD8
					&& (content[2] & 0xFF) == 0xFF;
			case ".png" -> startsWith(content, PNG_SIGNATURE);
			case ".txt" -> isUtf8Text(content);
			default -> false;
		};
		if (!valid) {
			throw new AttachmentRejectedException("ファイル内容が拡張子と一致しません");
		}
		return switch (extension) {
			case ".pdf" -> "application/pdf";
			case ".jpg", ".jpeg" -> "image/jpeg";
			case ".png" -> "image/png";
			case ".txt" -> "text/plain";
			default -> throw new AttachmentRejectedException("許可されていないファイル形式です");
		};
	}

	private static boolean startsWith(byte[] content, byte[] signature) {
		if (content.length < signature.length) {
			return false;
		}
		for (int index = 0; index < signature.length; index++) {
			if (content[index] != signature[index]) {
				return false;
			}
		}
		return true;
	}

	private static boolean isUtf8Text(byte[] content) {
		try {
			String text = StandardCharsets.UTF_8.newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT)
					.decode(ByteBuffer.wrap(content))
					.toString();
			return text.codePoints().noneMatch(codePoint ->
					Character.isISOControl(codePoint)
							&& codePoint != '\n'
							&& codePoint != '\r'
							&& codePoint != '\t'
							&& codePoint != '\f');
		} catch (CharacterCodingException exception) {
			return false;
		}
	}
}
