package com.zenfra.security;

public class Constants {

    public static final long ACCESS_TOKEN_VALIDITY_SECONDS = 5*60*60;
    public static final String SIGNING_KEY = "zenfra_migration";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    public static final String LOG_FILE_STATUS_DRAFT = "draft";
	public static final String LOG_FILE_STATUS_SUCCESS = "success";
	public static final String LOG_FILE_STATUS_FAILED = "failed";
	public static final String LOG_FILE_STATUS_PARSING = "parsing";
	public static final String LOG_FILE_STATUS_ERROR = "error";
	public static final String LOG_FILE_STATUS_QUEUE = "queue";
	public static final String LOG_FILE_STATUS_INSERTION = "insertion";
}
