package com.example.demo.service;

import java.io.IOException;

public interface VirusScanner {

	void scan(byte[] content) throws IOException;
}
