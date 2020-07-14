package com.github.marschwar.classDependency;

import java.io.IOException;
import java.io.Writer;

public interface ReportTransformer {
	void transform(Report report, Writer writer) throws IOException;
}
