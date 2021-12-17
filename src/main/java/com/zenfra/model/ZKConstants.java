package com.zenfra.model;

public class ZKConstants {
	
	public static String ZCAMUNDA = "ZCamunda";
	public static String CLOUD_PYTHON="/opt/ZENfra/repo/cloud-inventory-collectors/aws/authentication.py";
	public static String SERVER = "server";
	public static String PORT = "port";
	public static String DEFAULT_PATH = "defaultPath";
	public static String USER_MANAGEMENT = "UserManagement";
	public static String ZK_PROPERTIES_FILE_PATH = "/opt/config/server.properties";
	public static String STRING_FORMAT = "UTF-8";
	public static String DATEWISEREPORTROWS = "DateWiseReportRows";
	public static String PSRWEEKLYREPORTZEROROWS = "PSRWeeklyReportZeroRows";
	public static String PSRMONTHLYREPORTROWS = "PSRMonthlyReportRows";
	public static String PSRDAILYREPORTROWS = "PSRDailyReportRows";
	public static String ORIENTDBNAME = "orientdbname";
	public static String ORIENTDBPWD = "orientdbpwd";
	public static String ORIENTDBIP = "orientdbip";
	public static String ORIENTDBUSER = "orientdbuser";
	public static String INPUT_FOLDER = "inputFolder";
	public static String LOCALDISCOVERY = "LocalDiscovery";
	public static String SERVER_GROUP_BY = "serverGroupBy";
	public static String BLOCK_STORAGE_GROUP_BY = "blockStorageGroupBy";
	public static String FILE_STORAGE_GROUP_BY = "fileStorageGroupBy";
	public static String COMP_PINNED_COLUMNS = "compPinnedColumns";
	public static String ORIENTDBJDBC = "orientdbjdbc";
	public static String INDEXFOLDER = "indexfolder";
	public static String REPORTFOLDER = "reportfolder";
	public static String ZENFRA_V2_PORT = "zenfraV2Port";
	public static String DETAILED_REPORT = "DetailedReport";
	public static String COMPATIBILITY_BASE_FILTER = "CompatibilityBaseFilter";
	public static String DEFAULT_PROJECT = "defaultProject";
	public static String STORAGE_LIST = "storageList";
	public static String SERVER_LIST = "serverList";
	public static String SWITCH_LIST = "switchList";
	public static String USER_ORDER = "userOrder";
	public static String MENU = "menu";
	public static String DESTINATION_TEMPLATE_ORDER = "destinationTemplateOrder";
	public static String SITE_ORDER = "siteOrder";
	public static String MANAGE_SITE_ORDER = "manageSiteOrder";
	public static String PROJECT_ORDER = "projectOrder";
	public static String TASK_LIST_ORDER = "taskListOrder";
	public static String LOCK_PINNED = "lockPinned";
	public static String PINNED = "pinned";
	public static String LOCK_POSITION = "lockPosition";
	public static String TASKLIST_FIELD_TYPE = "fieldType";
	public static String VISIBLE = "visible";
	public static String ACTIVE = "active";
	public static String DESTINATION_PROFILE_ORDER = "destinationProfileOrder";
	public static String MANAGE_USER_ORDER = "manageUserOrder";
	public static String POLICY_ORDER = "policyOrder";
	public static String CUSTOM = "custom";
	public static String DISPLAY_LABLE = "displayLabel";
	public static String ORIENTDB_DRIVER_CLASS = "orientdb_driver_class";
	
	public static String RESET_URL = "resetUrl";
	public static String CREATEPASSWORD_URL = "createPasswordUrl";
	public static String SEND_MAIL_URL = "sendMailUrl";
	public static String RESET_LINK_ALIVE_TIME_IN_MINS = "resetLinkAliveTimeInMins";
	public static String MAIL_TEMPLATE_NAME_FOR_RESET_PASSWORD = "mailTemplateNameForResetPassword";
	public static String MAIL_TEMPLATE_NAME_FOR_USER_ACTIVATION = "mailTemplateNameForUserActivation";
	public static String MAIL_TEMPLATE_NAME_FOR_CREATE_PASSWORD = "mailTemplateNameForCreatePassword";
	public static String MAIL_TEMPLATE_NAME_FOR_RESET_SUCCESS = "mailTemplateNameForResetSuccess";
	public static String MAIL_TEMPLATE_NAME_FOR_ADMIN_RESET_PASSWORD = "mailTemplateNameForAdminResetPassword";
	public static String REDIS_HOST = "redis_host";
	public static String REDIS_PORT = "redis_port";
	public static String CACHETIME = "cacheTime";
	public static String CACHETIMEFORMAT = "cacheTimeFormat";
	
	
	
	
	public static String TASKLIST_DEFAULT_FIELDS = "taskDefaultFields";
	public static String CRCOLUMNNAMES = "CR_Columnnames";
	public static String CRDevice = "CR_Device";
	public static String THIRDPARTYIMPORTBASEPATH = "thirdpartyimportbasepath";
	public static String THIRDPARTYIMPORTFILEPATH = "thirdpartyimportfilepath";
	public static String CUSTOMDATAIMPORTBASEPATH = "customdataimportbasepath";
	public static String CUSTOMDATAIMPORTFILEPATH = "customdataimportfilepath";
	public static String DEFAULT_APP_PASSWORD = "default_app_password";
	public static String APP_SERVER_PROTOCOL = "appServerProtocol";
	public static String APP_SERVER_IP = "appServerIP";
	public static String APP_SERVER_PORT = "appServerPort";
	public static String COMPATIBILITY_COlUMNS = "compatibility_columns";
	
	public static String POSTGRES_URL = "postgresurl";
	public static String POSTGRES_USER = "postgres_user";
	public static String POSTGRES_PWD = "postgres_pwd";
	public static String ZENFR_FEATURES_CONTEXT_PATH = "zenfra-features";
	public static String pg_db_url="pg_db_url";
	public static String pg_db_port="pg_db_port";

	public static String parsing_server_ip="parsingServerIP";
	public static String parsingServerPort="parsingServerPort";
	public static String aws_data_script_path="aws_data_script_path";
	public static String aws_test_connection_path="aws_test_connection_path";
	public static String parsingServerProtocol="parsingServerProtocol";
	

	public static String FTP_FILE_COMLETE_MAILL_TEMPLATE_SUCCESS="ftp_template_success";
	public static String FTP_FILE_COMLETE_MAILL_TEMPLATE_FAIL="ftp_template_failure";
	public static String FTP_FILE_COMLETE_MAILL_TEMPLATE_PARTIALLY_PROCESSED="ftp_template_partially_processed";
	public static String MAIL_SERVICE_URL="mailServiceUrl";
	
	public static String POSTGRES_DRIVER_CLASS_NAME="pg_driver_class_name";
	public static String PG_MIN_IDLE_TIMEOUT="pg_min_idle_timeout";
	public static String PG_MAX_POOL_SIZE="pg_max_pool_size";
	public static String AWS_TABLE_JDBC_URL="postgresurl_aws_db";
	
	public final static String ERROR = "Error";
	public final static String SUCCESS = "Success";
	public final static String PARAMETER_MISSING = "Parameter missing";
	public static String HEALTHCHECK_COLUMN_ORDER = "validationRuleName,componentType,reportName,createdBy,createdTime,updatedBy,updatedTime";
	public static String HEADER_LABEL = "headerLabel";
	
	
	public static String PURE_File_PROP = "pureFileProp";
	public static String UPLOADEDLOGS_PATH = "uploadedlogs"; 
	public static String OUTPUTFOLDER_PATH = "outputlogs"; 
	public static String INSERTION_LOGS = "insertionlogs";
	public static String INSERTION_SCRIPT_FOLDER = "insertion_script_folder";
	public static String CUSTOM_REPORT_PROP = "customDiscoveryReportProp";
	public static String LOG_ANALYTICS_PATH = "log_analytics_path";
	public static String UNCOMPRESS_PATH = "uncompressPath";

	public static String PREDICTION_PATH = "predictionPath";
}
