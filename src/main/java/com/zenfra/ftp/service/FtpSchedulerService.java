package com.zenfra.ftp.service;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException.Unauthorized;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.icu.util.TimeZone;
import com.zenfra.configuration.AESEncryptionDecryption;
import com.zenfra.configuration.FTPClientConfiguration;
import com.zenfra.dao.common.CommonEntityManager;
import com.zenfra.ftp.repo.FtpSchedulerRepo;
import com.zenfra.model.LogFileDetails;
import com.zenfra.model.ftp.FTPServerModel;
import com.zenfra.model.ftp.FileNameSettingsModel;
import com.zenfra.model.ftp.FileWithPath;
import com.zenfra.model.ftp.FtpScheduler;
import com.zenfra.model.ftp.ProcessingStatus;
import com.zenfra.service.ProcessService;
import com.zenfra.service.UserCreateService;
import com.zenfra.utils.CommonFunctions;
import com.zenfra.utils.CommonUtils;
import com.zenfra.utils.Constants;
import com.zenfra.utils.Contants;
import com.zenfra.utils.DBUtils;
import com.zenfra.utils.ExceptionHandlerMail;
import scala.collection.immutable.HashSet;
import scala.collection.immutable.Set;

@Service
public class FtpSchedulerService extends CommonEntityManager {

    @Autowired
    FtpSchedulerRepo repo;

    @Autowired
    FileNameSettingsService settingsService;

    @Autowired
    FTPClientService clientService;

    @Autowired
    CommonFunctions functions;

    @Autowired
    ProcessService process;

    @Autowired
    AESEncryptionDecryption encryption;

    @Autowired
    UserCreateService userCreateService;

//    Set<String> nasLogFileNameSet = new HashSet<>();
//    Set<String> nasLogTypeSet = new HashSet<>();
    public long saveFtpScheduler(FtpScheduler ftpScheduler) {

        try {

            repo.save(ftpScheduler);
            repo.flush();

            System.out.println("----get id-----" + ftpScheduler.getId());
            return ftpScheduler.getId();
        } catch (Exception e) {
            e.printStackTrace();
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            String ex = errors.toString();
            ExceptionHandlerMail.errorTriggerMail(ex);
            return 0;
        }
    }

    public FtpScheduler getFtpScheduler(String fileNameSettingsId) {

        try {

            return repo.findAllById(fileNameSettingsId);
        } catch (Exception e) {
            e.printStackTrace();
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            String ex = errors.toString();
            ExceptionHandlerMail.errorTriggerMail(ex);
            return null;
        }
    }

    public List<FtpScheduler> getFtpSchedulerAll() {

        try {

            return repo.findAll();
        } catch (Exception e) {
            e.printStackTrace();
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            String ex = errors.toString();
            ExceptionHandlerMail.errorTriggerMail(ex);
            return null;
        }
    }

    public Object runFtpSchedulerFiles(FtpScheduler s) throws SQLException {
        ProcessingStatus status = new ProcessingStatus();
        ProcessService process = new ProcessService();
        JSONObject email = new JSONObject();
        CommonFunctions functions = new CommonFunctions();
        ObjectMapper mapper = new ObjectMapper();
        String passFileList = "";
        FTPServerModel server = new FTPServerModel();
        final List<FileWithPath> files = new ArrayList<>();
        Map<String, String> values = DBUtils.getEmailURL();
        try {
            System.out.println("--------------eneter runFtpSchedulerFiles---------" + s.toString());
            List<String> l = new ArrayList<String>();
            if (s.getEmailString() != null && s.getEmailString() != "[]") {
                String arr[] = s.getEmailString().replace("\"", "").replace("[", "").replace("]", "").split(",");
                if (arr.length > 0) {
                    Collections.addAll(l, arr);
                }
            }
            Map<String, Object> userMap = getObjectByQueryNew(
                    "select * from user_temp where user_id='" + s.getUserId() + "'");
            email.put("mailFrom", userMap.get("email").toString());
            email.put("mailTo", l);
            email.put("firstName", userMap.get("first_name").toString());
            // email.put("Time", functions.getCurrentDateWithTime());
            email.put("Notes", "File processing initiated");
            email.put("ftp_template", values.get("ftp_template_success_02"));
            FileNameSettingsService settingsService = new FileNameSettingsService();
            System.out.println("s.getFileNameSettingsId()::" + s.getFileNameSettingsId());

            String getFileNameSettings = "select * from file_name_settings_model where file_name_setting_id='"
                    + s.getFileNameSettingsId() + "'";
            FileNameSettingsModel settings = new FileNameSettingsModel();
            Map<String, Object> map = getObjectByQueryNew(getFileNameSettings);// settingsService.getFileNameSettingsById(s.getFileNameSettingsId());
            if (map != null) {
                settings.setFileNameSettingId(map.get("file_name_setting_id").toString());
                settings.setFtpName(map.get("ftp_name").toString());
                settings.setIpAddress(map.get("ip_address").toString());
                System.out.println(map.get("pattern_string"));
                settings.setPattern(map.get("pattern_string") != null && !map.get("pattern_string").toString().isEmpty()
                        ? mapper.readValue(map.get("pattern_string").toString(), JSONArray.class)
                        : new JSONArray());
                settings.setSiteKey(map.get("site_key").toString());
                settings.setToPath(map.get("to_path").toString());
                settings.setUserId(map.get("user_id").toString());
            }

            System.out.println("settings::" + settings.toString());
            String serverQuery = "select * from ftpserver_model  where site_key='" + settings.getSiteKey()
                    + "' and ftp_name='" + settings.getFtpName() + "'";
            Map<String, Object> serverMap = getObjectByQueryNew(serverQuery);// settingsService.getFileNameSettingsById(s.getFileNameSettingsId());

            if (server != null) {
                server.setFtpName(serverMap.get("ftp_name").toString());
                server.setIpAddress(serverMap.get("ip_address").toString());
                server.setPort(serverMap.get("port").toString());
                server.setServerId(serverMap.get("server_id").toString());
                server.setServerPassword(serverMap.get("server_password").toString());
                server.setServerPath(serverMap.get("server_path").toString());
                server.setServerUsername(serverMap.get("server_username").toString());
                server.setSiteKey(serverMap.get("site_key").toString());
                server.setUserId(serverMap.get("user_id").toString());
            }
            email.put("subject", Constants.ftp_sucess.replace(":ftp_name", server.getFtpName()));
            email.put("FTPname", server.getFtpName());
            status.setProcessingType("FTP");
            status.setProcessing_id(functions.generateRandomId());
            status.setStartTime(functions.getCurrentDateWithTime());
            status.setProcessDataId(String.valueOf(server.getServerId()));
            status.setStatus("Scheduler start");
            status.setSiteKey(server.getSiteKey());
            status.setPath(server.getServerPath());
            status.setEndTime(functions.getCurrentDateWithTime());

            String processQuery = "INSERT INTO processing_status(processing_id, end_time, log_count, path, process_data_id, processing_type,  site_key, start_time, status, tenant_id, user_id)	VALUES (':processing_id', ':end_time',  ':log_count', ':path', ':process_data_id', ':processing_type', ':site_key', ':start_time', ':status', ':tenant_id', ':user_id');";

            processQuery = processQuery.replace(":processing_id", status.getProcessing_id())
                    .replace(":end_time", functions.getCurrentDateWithTime()).replace(":log_count", "0")
                    .replace(":path", server.getServerPath())
                    .replace(":process_data_id", String.valueOf(server.getServerId()))
                    .replace(":processing_type", "FTP").replace(":site_key", server.getSiteKey())
                    .replace(":start_time", functions.getCurrentDateWithTime()).replace(":status", "Scheduler started")
                    .replace(":tenant_id", "").replace(":user_id", server.getUserId());
            excuteByUpdateQueryNew(processQuery);
            files.addAll(settingsService.getFilesByPattern(server, settings));
            String processUpdate = "UPDATE processing_status SET log_count=':log_count',  status=':status' WHERE processing_id=':processing_id';";
            processUpdate = processUpdate.replace(":log_count", String.valueOf(files.size()))
                    .replace(":status", "Retrieving files").replace(":processing_id", status.getProcessing_id());
            excuteByUpdateQueryNew(processUpdate);

            System.out.println("FileWithPath size::" + files.size());

//            String token = functions.getZenfraToken(Constants.ftp_email, Constants.ftp_password);

            List<LogFileDetails> logFiles = new ArrayList<LogFileDetails>();
            String emailFileList = "";
            String updateFiles = "";
            for (FileWithPath file : files) {
//                System.out.println("Token::" + token);
                System.out.println("Final::" + file.getPath());
                LogFileDetails url = callParsing(file.getLogType(), settings.getUserId(), settings.getSiteKey(),
                        s.getTenantId(), file.getName(), "", file.getPath(), s.getId(), false);
                logFiles.add(url);
                emailFileList += "<li>" + file.getLogType() + ":" + file.getName() + "</li>";
                updateFiles += updateFiles + "," + file.getName();
            }
            String statusFtp = "File processing";
            if (emailFileList.isEmpty() || emailFileList == null) {
                emailFileList = "No files";
                statusFtp = "No file to process";
            }
            email.put("Time", functions.getCurrentDateWithTime() + " " + TimeZone.getDefault().getDisplayName());
            email.put("FileList", emailFileList);
            String processUpdateLast = "UPDATE processing_status SET file=':file',end_time=':end_time',status=':status' WHERE processing_id=':processing_id';";
            processUpdateLast = processUpdateLast.replace(":file", updateFiles)
                    .replace(":end_time", functions.getCurrentDateWithTime()).replace(":status", statusFtp)
                    .replace(":processing_id", status.getProcessing_id());
            excuteByUpdateQueryNew(processUpdateLast);

            if (files.size() > 0) {
                process.sentEmailFTP(email);
            }

            RestTemplate restTemplate = new RestTemplate();
            for (LogFileDetails logFile : logFiles) {
                passFileList += "<li>" + logFile.getFileName() + "</li>";
                Thread.UncaughtExceptionHandler h = new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread th, Throwable ex) {
                        email.put("ftp_template", values.get("ftp_template_partially_processed"));
                        email.put("FileList", "<li>" + logFile.getFileName() + "</li>");
                        email.put("subject",
                                Constants.ftp_Partially_Processed.replace(":ftp_name", server.getFtpName()));
                        email.put("Notes",
                                "Unable to process the file. Don't worry, Admin will check. The above listed files are processing fail.");
                        if (files.size() > 0) {
                            process.sentEmailFTP(email);
                        }
                    }
                };

            }
            return files;
        } catch (Exception e) {
            e.printStackTrace();
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            String ex = errors.toString();
            ExceptionHandlerMail.errorTriggerMail(ex);
            email.put("ftp_template", values.get("ftp_template_failure"));
            email.put("FileList", passFileList);
            email.put("subject", Constants.ftp_fail.replace(":ftp_name", server.getFtpName()));
            email.put("Notes",
                    "Unable to process the files. Don't worry, Admin will check. The above listed files are successfully processed.");
            if (files.size() > 0) {
                process.sentEmailFTP(email);
            }
            String processUpdateLast = "UPDATE processing_status SET response=':response',end_time=':end_time'  status=':status' WHERE processing_id=':processing_id';";
            processUpdateLast = processUpdateLast.replace(":response", e.getMessage())
                    .replace(":end_time", functions.getCurrentDateWithTime()).replace(":status", "Failed")
                    .replace(":processing_id", status.getProcessing_id());
            status.setEndTime(functions.getCurrentDateWithTime());
            status.setStatus("Failed");
            status.setResponse(e.getMessage());
            excuteByUpdateQueryNew(processUpdateLast);
            return status;
        }
    }

    public List<FileWithPath> getFilesBased(FTPServerModel server, FileNameSettingsModel settings) {

        try {
            return settingsService.getFilesByPattern(server, settings);
        } catch (Exception e) {
            e.printStackTrace();
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            String ex = errors.toString();
            ExceptionHandlerMail.errorTriggerMail(ex);
            return null;
        }
    }

    public LogFileDetails callParsing(String logType, String userId, String siteKey, String tenantId, String fileName,
                                      String token, String folderPath, long schedulerId, boolean isNas) {

        CommonFunctions functions = new CommonFunctions();
        LogFileDetails logFile = new LogFileDetails();
        try {

            File convFile = getFilePathFromFTP(folderPath, fileName);

            logFile.setLogFileId(functions.generateRandomId());
            logFile.setActive(true);
            logFile.setCreatedDateTime(functions.getCurrentDateWithTime());
            if (isNas) {
                logFile.setDescription("NAS file parsing");
            } else {
                logFile.setDescription("FTP file parsing");
            }
            logFile.setFileName(fileName);
            logFile.setFileSize(String.valueOf(convFile.length()));
            logFile.setLogType(logType);
            // logFile.setExtractedPath(folderPath + "/" + fileName);
            System.out.println("----FTP PATH------   " + convFile.getAbsolutePath());
            logFile.setExtractedPath(convFile.getAbsolutePath());
            logFile.setSiteKey(siteKey);
            logFile.setStatus(Contants.LOG_FILE_STATUS_QUEUE);
            logFile.setUpdatedDateTime(functions.getCurrentDateWithTime());
            logFile.setUploadedBy(userId);
            logFile.setTenantId(tenantId);
            logFile.setMasterLogs("");
            logFile.setMessage("");
            logFile.setParsingStatus("");
            logFile.setFilePaths("");

            /*
             * String query="INSERT INTO public.log_file_details(log_file_id, " +
             * "created_date_time, description, " +
             * " file_name, file_size, is_active, log_type, master_logs,  site_key, status, tenant_id,"
             * + " updated_date_time, uploaded_by, message, " +
             * " parsing_status, file_paths,extracted_path)" +
             * " values('"+logFile.getLogFileId()+"','"+logFile.getCreatedDateTime()+"','"+
             * logFile.getDescription()+"','"+logFile.getFileName()+"','"+logFile.
             * getFileSize()+"'" +
             * ","+logFile.getActive()+",'"+logFile.getLogType()+"','"+logFile.getMasterLogs
             * ()+"','"+logFile.getSiteKey()+"','"+logFile.getStatus()+"','"+logFile.
             * getTenantId()+"','"+logFile.getUpdatedDateTime()+"'," +
             * " '"+logFile.getUploadedBy()+"','"+logFile.getMessage()+"','"+logFile.
             * getParsingStatus()+"','"+logFile.getFilePaths()+"','"+logFile.
             * getExtractedPath()+"')";
             *
             * System.out.println("insert log_file_details query::"+query);
             *
             * excuteByUpdateQueryNew(query);
             */
            String parsingURL = DBUtils.getParsingServerIP();
            RestTemplate restTemplate = new RestTemplate();
            System.out.println("Enter Parsing.....");
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("parseFilePath", folderPath);
            body.add("parseFileName", fileName);
            body.add("isFTP", true);
            body.add("logType", logType);
            if (isNas) {
                body.add("description", "NAS file parsing");
            } else {
                body.add("description", "FTP file parsing");
            }
            body.add("siteKey", siteKey);
            body.add("userId", userId);
            body.add("tenantId", tenantId);
            body.add("uploadAndProcess", true);

            System.out.println("Params::" + body);
            HttpEntity<Object> request = new HttpEntity<>(body, createHeaders("Bearer " + token));
            String uri = parsingURL + "/parsing/upload";
            uri = CommonUtils.checkPortNumberForWildCardCertificate(uri);

            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, request, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());

            // String rid, String logType, String description, boolean isReparse
            if (root == null) {
                System.out.println("invalid response");
            }
            System.out.println("Upload response::" + response);
            // final String rid =
            // root.get("jData").get("logFileDetails").get(0).get("rid").toString().replace("\"",
            // "");

            /*
             * StringBuilder builder = new StringBuilder(parsingURL+"/parsing/parse");
             * builder.append("?logFileId=");
             * builder.append(URLEncoder.encode(logFile.getLogFileId(),StandardCharsets.
             * UTF_8.toString())); builder.append("&logType=");
             * builder.append(URLEncoder.encode(logType,StandardCharsets.UTF_8.toString()));
             * builder.append("&description=");
             * builder.append(URLEncoder.encode("",StandardCharsets.UTF_8.toString()));
             * builder.append("&isReparse=");
             * builder.append(URLEncoder.encode("false",StandardCharsets.UTF_8.toString()));
             */

        } catch (Exception e) {
            e.printStackTrace();
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            String ex = errors.toString();
            ExceptionHandlerMail.errorTriggerMail(ex);
        }

        return logFile;
    }

    HttpHeaders createHeaders(String token) {
        return new HttpHeaders() {
            {
                set("Authorization", token);
                setContentType(MediaType.MULTIPART_FORM_DATA);
            }
        };
    }

    public List<String> getFilesFromFolder(String path) {
        List<String> listFiles = new ArrayList<String>();
        try {

            System.out.println("Set path:: " + path);
            File Folder = new File(path);
            System.out.println("Folder:: " + Folder);
            for (File filentry : Folder.listFiles()) {
                listFiles.add(filentry.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            String ex = errors.toString();
            ExceptionHandlerMail.errorTriggerMail(ex);
        }
        return listFiles;
    }

    public void CallFTPParseAPI(RestTemplate restTemplate, String builder, String token) {

        try {

            URI uri = URI.create(builder.toString());
            System.out.println("Parisng call::" + uri);
            HttpEntity<Object> requestParse = new HttpEntity<>(createHeaders("Bearer " + token));
            ResponseEntity<String> responseParse = restTemplate
                    // .exchange("http://localhost:8080/usermanagment/rest/ftpScheduler",
                    // HttpMethod.POST, request, String.class);
                    .exchange(uri, HttpMethod.GET, requestParse, String.class);

        } catch (Unauthorized e) {
            e.printStackTrace();
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            String ex = errors.toString();
            ExceptionHandlerMail.errorTriggerMail(ex);
            token = functions.getZenfraToken(Constants.ftp_email, Constants.ftp_password);
            CallFTPParseAPI(restTemplate, builder, token);
        } catch (Exception e) {
            e.printStackTrace();
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            String ex = errors.toString();
            ExceptionHandlerMail.errorTriggerMail(ex);
        }
    }

    public File getFilePathFromFTP(String folderPath, String filePath) {

        try {
            File inputFolder = new File(folderPath);
            if (!inputFolder.exists()) {
                inputFolder.mkdirs();
            }

            File convFile = new File(filePath);
            return convFile;
        } catch (Exception e) {

        }

        return null;
    }

    public Object parseNasFiles(FtpScheduler s) throws SQLException {
        ProcessingStatus status = new ProcessingStatus();
        ArrayList<File> files = new ArrayList<File>();
        ProcessService process = new ProcessService();
        JSONObject email = new JSONObject();
        CommonFunctions functions = new CommonFunctions();
        ObjectMapper mapper = new ObjectMapper();
        JSONArray patternArray = new JSONArray();
        String passFileList = "";
        FTPServerModel server = new FTPServerModel();
        FileNameSettingsService settingsService = new FileNameSettingsService();
        System.out.println("s.getFileNameSettingsId()::" + s.getFileNameSettingsId());
        Map<String, String> values = DBUtils.getEmailURL();
        Map<String, String> data = new HashMap<>();
        data = DBUtils.getPostgres();
        try (Connection connection = DriverManager.getConnection(data.get("url"), data.get("userName"),
                data.get("password"));
             Statement statement = connection.createStatement();
             Statement statement1 = connection.createStatement();
             Statement statement2 = connection.createStatement();
             Statement statement3 = connection.createStatement();
             Statement statement4 = connection.createStatement();) {
            System.out.println("--------------Eneter nas Nas Scheduler Files---------" + s.toString());
            List<String> l = new ArrayList<String>();
            if (s.getEmailString() != null && s.getEmailString() != "[]") {
                String arr[] = s.getEmailString().replace("\"", "").replace("[", "").replace("]", "").split(",");
                if (arr.length > 0) {
                    Collections.addAll(l, arr);
                }
            }
            String selectQuery = "select * from user_temp where user_id='" + s.getUserId() + "'";
            System.out.println("!!!selectQuery: " + selectQuery);
            ResultSet rs = statement.executeQuery(selectQuery);
            while (rs.next()) {
                email.put("mailFrom", rs.getString("email").toString());
                email.put("mailTo", l);
                email.put("firstName", rs.getString("first_name").toString());
                email.put("Notes", "File processing initiated");
                email.put("ftp_template", values.get("ftp_template_success"));
            }

            FileNameSettingsModel settings = new FileNameSettingsModel();
            String getFileNameSettings = "select * from file_name_settings_model where file_name_setting_id='"
                    + s.getFileNameSettingsId() + "'";
            System.out.println("!!!GetFileNameSettings: " + getFileNameSettings);

            ResultSet rs1 = statement1.executeQuery(getFileNameSettings);
            while (rs1.next()) {
                settings.setFileNameSettingId(rs1.getString("file_name_setting_id").toString());
                settings.setFtpName(rs1.getString("ftp_name").toString());
                settings.setIpAddress(rs1.getString("ip_address").toString());
                System.out.println("!!!Pattern String: "+rs1.getString("pattern_string"));
                try {
                    settings.setPattern(rs1.getString("pattern_string") != null
                            && !rs1.getString("pattern_string").toString().isEmpty()
                            ? mapper.readValue(rs1.getString("pattern_string").toString(), JSONArray.class)
                            : new JSONArray());
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                settings.setSiteKey(rs1.getString("site_key").toString());
                settings.setToPath(rs1.getString("to_path").toString());
                settings.setUserId(rs1.getString("user_id").toString());
            }
            String serverQuery = "select * from ftpserver_model  where site_key='" + settings.getSiteKey()
                    + "' and ftp_name='" + settings.getFtpName() + "' and is_nas = true";
            System.out.println("!!!GetFileNameSettings: " + serverQuery);

            ResultSet rs2 = statement2.executeQuery(serverQuery);
            while (rs2.next()) {
                server.setFtpName(rs2.getString("ftp_name").toString());
                server.setIpAddress(rs2.getString("ip_address").toString());
                server.setPort(rs2.getString("port").toString());
                server.setServerId(rs2.getString("server_id").toString());
                server.setServerPassword(rs2.getString("server_password").toString());
                server.setServerPath(rs2.getString("server_path").toString());
                server.setServerUsername(rs2.getString("server_username").toString());
                server.setSiteKey(rs2.getString("site_key").toString());
                server.setUserId(rs2.getString("user_id").toString());
                server.setNas(true);
            }

            email.put("subject", Constants.nas_sucess.replace(":ftp_name", server.getFtpName()));
            email.put("FTPname", server.getFtpName());
            status.setProcessingType("NAS");
            status.setProcessing_id(functions.generateRandomId());
            status.setStartTime(functions.getCurrentDateWithTime());
            status.setProcessDataId(String.valueOf(server.getServerId()));
            status.setStatus("Scheduler start");
            status.setSiteKey(server.getSiteKey());
            status.setPath(server.getServerPath());
            status.setEndTime(functions.getCurrentDateWithTime());
            String processQuery;

            if (s.getIsNas() && settings.getPattern().size() > 0) {
                System.out.println("---isnas--" + s.getIsNas());
                processQuery = "INSERT INTO processing_status(processing_id, end_time, log_count, path, process_data_id, processing_type,  site_key, start_time, status, tenant_id, user_id, is_nas)	VALUES (':processing_id', ':end_time',  ':log_count', ':path', ':process_data_id', ':processing_type', ':site_key', ':start_time', ':status', ':tenant_id', ':user_id', true);";
                processQuery = processQuery.replace(":processing_id", status.getProcessing_id())
                        .replace(":end_time", functions.getCurrentDateWithTime()).replace(":log_count", "0")
                        .replace(":path", server.getServerPath())
                        .replace(":process_data_id", String.valueOf(server.getServerId()))
                        .replace(":processing_type", "NAS").replace(":site_key", server.getSiteKey())
                        .replace(":start_time", functions.getCurrentDateWithTime()).replace(":status", "Scheduler started")
                        .replace(":tenant_id", "").replace(":user_id", server.getUserId());
                statement3.executeUpdate(processQuery);
            }
            System.out.println("FileWithPath size::" + files.size());
            files.addAll(getNasFiles(server, s, settings, email));
            System.out.println("!!!Files size: " + files.size());
            if (files.size() > 0) {
                String processUpdate = "UPDATE processing_status SET log_count=':log_count',  status=':status' WHERE processing_id=':processing_id';";
                processUpdate = processUpdate.replace(":log_count", String.valueOf(files.size()))
                        .replace(":status", "Successfully Processed").replace(":processing_id", status.getProcessing_id());
                statement4.executeUpdate(processUpdate);
            }
 //           process.sentEmailFTP(email);
            RestTemplate restTemplate = new RestTemplate();
            for (File logFile : files) {
                passFileList += "<li>" + logFile.getName() + "</li>";
                Thread.UncaughtExceptionHandler h = new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread th, Throwable ex) {
                        email.put("ftp_template", values.get("ftp_template_partially_processed"));
                        email.put("FileList", "<li>" + logFile.getName() + "</li>");
                        email.put("subject",
                                Constants.ftp_Partially_Processed.replace(":ftp_name", server.getFtpName()));
                        email.put("Notes",
                                "Unable to process the file. Don't worry, Admin will check. The above listed files are processing fail.");
                    }
                };
            }
            System.out.println("****Nas Scheduler Completed****");
            System.out.println("!!!Status: "+status.getStatus());
            return status;
        } catch (Exception e) {
            e.printStackTrace();
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            email.put("ftp_template", values.get("ftp_template_failure"));
            email.put("FileList", passFileList);
            email.put("subject", Constants.ftp_fail.replace(":ftp_name", server.getFtpName()));
            email.put("Notes",
                    "Unable to process the files. Don't worry, Admin will check. The above listed files are successfully processed.");
            if (files.size() > 0) {
                process.sentEmailFTP(email);
            }
            String processUpdateLast = "UPDATE processing_status SET response=':response',end_time=':end_time'  status=':status' WHERE processing_id=':processing_id';";
            processUpdateLast = processUpdateLast.replace(":response", e.getMessage())
                    .replace(":end_time", functions.getCurrentDateWithTime()).replace(":status", "Failed")
                    .replace(":processing_id", status.getProcessing_id());
            status.setEndTime(functions.getCurrentDateWithTime());
            status.setStatus("Failed");
            status.setResponse(e.getMessage());
            excuteByUpdateQueryNew(processUpdateLast);
            return status;
        }
    }


    private ArrayList<File> getNasFiles(FTPServerModel server, FtpScheduler s, FileNameSettingsModel settings, JSONObject email) {
        ArrayList<File> files = null;
        FTPClientConfiguration ftpClientConfiguration = new FTPClientConfiguration();
        FileNameSettingsService fileNameSettingsService = new FileNameSettingsService();
        ProcessService process = new ProcessService();
        CommonFunctions functions = new CommonFunctions();
        String patternVal = null;
        String logType1 = null;
        Map<String, Object> map = new HashMap<String, Object>();
        ObjectMapper map1 = new ObjectMapper();
        long bytes;
        long fileSize;
        String emailFileList = "";
        String statusFtp = "";
        try {
            System.out.println("!!!Settings Pattern: " + settings.getPattern());
            System.out.println("!!!server Path: " + server.getServerPath());
            final File folder = new File(server.getServerPath().toString());
            for (int j = 0; j < settings.getPattern().size(); j++) {
                JSONObject patJson = map1.convertValue(settings.getPattern().get(j), JSONObject.class);
                patternVal = patJson.get("namePattern").toString().replace("*", ".*");
                logType1 = patJson.get("logType").toString().replace("*", ".*");
                System.out.println("!!!PatternVal: " + patternVal);
                System.out.println("!!!Pattern LogType: " + logType1);
                files = nasListFilesForFolder(folder, server, patternVal);
                System.out.println("File List: " + files);
                Map<String, List<String>> existCheckSums = ftpClientConfiguration.getCheckSumDetails(server.getSiteKey());
                for (File file : files) {
                    System.out.println("!!!File Name: " + file.getName());
                    if (fileNameSettingsService.isValidMatch(patternVal, file.getName())){
                        bytes = file.length();
                        fileSize = (bytes / 1024);
                        map.put("serverId", server.getServerId());
                        map.put("fileName", file.getName());
                        map.put("siteKey", server.getSiteKey());
                        map.put("fileSize", String.valueOf(fileSize));
                        System.out.println("!!!DB File Size: " + map.get("fileSize"));
                        System.out.println("!!!Check Sum Map: " + map);
                        if (ftpClientConfiguration.copyStatusNas(map, existCheckSums)) {
                            System.out.println("****** File already present ******");
                        } else {
                            emailFileList += "<li>" + logType1 + ":" + file.getName() + "</li>";
                            System.out.println("!!!Email File List" + emailFileList);
                            System.out.println("****!!!File Pattern Found!!!****");
                            callParsing(logType1, s.getUserId(), s.getSiteKey(), s.getTenantId(), file.getName(), "",
                                    folder.getAbsolutePath(), s.getId(), server.isNas);
                            System.out.println("****!!!File Pattern Not Found!!!****");
                        }
                    }
                }
            }
            statusFtp = "File processing";
            if (emailFileList.isEmpty() || emailFileList == null) {
                emailFileList = "No files";
                statusFtp = "No file to process";
            }
            email.put("Time", functions.getCurrentDateWithTime() + " " + TimeZone.getDefault().getDisplayName());
            email.put("FileList", emailFileList);
            if (files.size() > 0) {
                process.sentEmailFTP(email);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return files;

    }


    public ArrayList<File> nasListFilesForFolder(final File folder, FTPServerModel server, String patternVal) {
        ArrayList<File> fileList = new ArrayList<File>();
        FileNameSettingsService fileNameSettingsService = new FileNameSettingsService();
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                nasListFilesForFolder(fileEntry, server, patternVal);
                if (fileNameSettingsService.isValidMatch(patternVal, fileEntry.getName())){
                    fileList.add(fileEntry.getAbsoluteFile());
                }
            } else {
                if (fileNameSettingsService.isValidMatch(patternVal, fileEntry.getName())){
                    fileList.add(fileEntry.getAbsoluteFile());
                }
            }
        }
        return fileList;
    }
}
