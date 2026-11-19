package com.example.demo.service;

public class VirusDetectedException extends AttachmentRejectedException {

	public VirusDetectedException() {
		super("ウイルスが検出されたため、添付ファイルを拒否しました");
	}
}
