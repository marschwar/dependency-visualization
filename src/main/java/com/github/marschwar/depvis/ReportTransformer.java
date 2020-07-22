package com.github.marschwar.depvis;

import java.io.IOException;
import java.io.Writer;

public interface ReportTransformer {

	void transform(Report report, Writer writer) throws IOException;

	String getFileExtension();
}
