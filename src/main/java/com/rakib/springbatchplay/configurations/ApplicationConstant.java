package com.rakib.springbatchplay.configurations;

public class ApplicationConstant {
    public static final String BATCH_FILE_PATH_PARAMETER = "input.file.path";
    public static final String BATCH_FILE_PATH_PARAMETER_FULL = "#{jobParameters['" + BATCH_FILE_PATH_PARAMETER + "']}";
}
