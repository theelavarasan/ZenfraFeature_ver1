package com.zenfra.dataframe.util;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.types.ArrayType;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import com.zenfra.dataframe.response.DataResult;
import com.zenfra.utils.ExceptionHandlerMail;

import scala.collection.JavaConverters;
import scala.collection.Seq;

public class DataframeUtil {

	public static String asJsonResponse(DataResult result) {
		//String secondaryColumns = result.getSecondaryColumns().isEmpty() ? ""
		//		: "\"" + String.join("\", \"", result.getSecondaryColumns()) + "\"";
//
		return "{" + "\"data\": [" + String.join(",", result.getData()) + "], " + "\"lastRow\":" + result.getLastRow()
				+ ", " + "\"totalCount\": " + result.getTotalRecord() +
				/* "\"unit_conv_details\": " + result.getUnit_conv_details() + "" + */
			    ", " + "\"countData\": " + result.getCountData() +
				"}";
	}

	public static Dataset flattenJSONdf(Dataset<Row> ds) {

		StructField[] fields = ds.schema().fields();

		List<String> fieldsNames = new ArrayList<>();
		for (StructField s : fields) {
			fieldsNames.add(s.name());
		}

		for (int i = 0; i < fields.length; i++) {

			StructField field = fields[i];
			DataType fieldType = field.dataType();
			String fieldName = field.name();

			if (fieldType instanceof ArrayType) {
				List<String> fieldNamesExcludingArray = new ArrayList<String>();
				for (String fieldName_index : fieldsNames) {
					if (!fieldName.equals(fieldName_index))
						fieldNamesExcludingArray.add(fieldName_index);
				}

				List<String> fieldNamesAndExplode = new ArrayList<>(fieldNamesExcludingArray);
				String s = String.format("explode_outer(%s) as %s", fieldName, fieldName);
				fieldNamesAndExplode.add(s);

				String[] exFieldsWithArray = new String[fieldNamesAndExplode.size()];
				Dataset exploded_ds = ds.selectExpr(fieldNamesAndExplode.toArray(exFieldsWithArray));

				// explodedDf.show();

				return flattenJSONdf(exploded_ds);

			} else if (fieldType instanceof StructType) {

				String[] childFieldnames_struct = ((StructType) fieldType).fieldNames();

				List<String> childFieldnames = new ArrayList<>();
				for (String childName : childFieldnames_struct) {
					childFieldnames.add(fieldName + "." + childName);
				}

				List<String> newfieldNames = new ArrayList<>();
				for (String fieldName_index : fieldsNames) {
					if (!fieldName.equals(fieldName_index))
						newfieldNames.add(fieldName_index);
				}

				newfieldNames.addAll(childFieldnames);

				List<Column> renamedStrutctCols = new ArrayList<>();

				for (String newFieldNames_index : newfieldNames) {
					renamedStrutctCols.add(new Column(newFieldNames_index.toString())
							.as(newFieldNames_index.toString().replace(".", "_")));
				}

				Seq renamedStructCols_seq = JavaConverters.collectionAsScalaIterableConverter(renamedStrutctCols)
						.asScala().toSeq();

				Dataset ds_struct = ds.select(renamedStructCols_seq);

				return flattenJSONdf(ds_struct);
			} else {

			}

		}
		return ds;
	}

	public static Seq<String> convertListToSeq(List<String> inputList) {
		return JavaConverters.asScalaIteratorConverter(inputList.iterator()).asScala().toSeq();
	}

	public static void formatJsonFile(File[] files) {
		try {
			for (File file : files) {
				if (file.isDirectory()) {
					formatJsonFile(file.listFiles());
				} else {

					if (file.getPath().endsWith(".crc")) {
						file.delete();
					} else if (file.getPath().endsWith(".json")) {
						jsonFormatHanlder(file.getPath());
					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace();			
		}

	}
	
	public static void jsonFormatHanlder(String filePath) {
		try {
			Path path = Paths.get(filePath);
			Stream<String> lines = Files.lines(path);  //.replaceAll("\\\\\\\\\\\\\\\"","")  //.replaceAll("\"(-?\\d+(?:[\\.,]\\d+)?)\"", "$1")
		List<String> replaced = lines.map(line -> line.replaceAll("\\\\", "")
					.replaceAll(":\"\\[\\{", ":\"\",").replaceAll("\\}\\]\"", "")
					.replaceAll(":\\[\\{", ":\"\",").replaceAll("\\}\\]", "")).collect(Collectors.toList());
			
			
			Files.write(path, replaced);
			lines.close();
			
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Dataset<Row> renameDataFrameColumn(Dataset<Row> dataset, String columnName, String aliasName) {
		for (String column : dataset.columns()) {
			dataset = dataset.withColumnRenamed(column, column.replace(columnName, aliasName));
		}
		return dataset;
	}

	public static List<String> getAllFileNamesByType(Path path, String type) {
		if (!Files.isDirectory(path)) {
			throw new IllegalArgumentException("Path must be a directory!");
		}

		List<String> result = null;

		try (Stream<Path> walk = Files.walk(path)) {
			result = walk.filter(p -> !Files.isDirectory(p)).map(p -> p.toString()).filter(f -> f.endsWith(type))
					.collect(Collectors.toList());
		} catch (Exception e) {

		}

		return result;
	}

	public static boolean deleteFile(String filePath) {
		boolean result = false;
		try {
			File f = new File(filePath);
			f.setExecutable(true, false);
			f.setWritable(true, false);
			f.setReadable(true, false);
			f.delete();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

		return result;
	}

	public static void deleteAllFilesInFolder(String path) {
		try {
			File folder = new File(path);
			for (File file : folder.listFiles()) {
				file.delete();
			}
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

	}

	public static void setFilePermission(String pathName) {

		try {
			Path path = Paths.get(pathName);
			Set<PosixFilePermission> perms = Files.readAttributes(path, PosixFileAttributes.class).permissions();

			System.out.format("Permissions before: %s%n", PosixFilePermissions.toString(perms));

			perms.add(PosixFilePermission.OWNER_WRITE);
			perms.add(PosixFilePermission.OWNER_READ);
			perms.add(PosixFilePermission.OWNER_EXECUTE);
			perms.add(PosixFilePermission.GROUP_WRITE);
			perms.add(PosixFilePermission.GROUP_READ);
			perms.add(PosixFilePermission.GROUP_EXECUTE);
			perms.add(PosixFilePermission.OTHERS_WRITE);
			perms.add(PosixFilePermission.OTHERS_READ);
			perms.add(PosixFilePermission.OTHERS_EXECUTE);
			Files.setPosixFilePermissions(path, perms);

		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

	}

	public static void executeUnixCommand(String cmd) {
		try {
			Process p = Runtime.getRuntime().exec(cmd);
			p.waitFor();
			System.out.println("exit: " + p.exitValue());
			p.destroy();
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}

	}

	public static boolean changeOwnerForFile(String path) {
		try {
			Path p = Paths.get(path);
			String groupName = "GROUP_NAME";
			UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
			GroupPrincipal group = lookupService.lookupPrincipalByGroupName(groupName);
			Files.getFileAttributeView(p, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setGroup(group);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			String ex = errors.toString();
			ExceptionHandlerMail.errorTriggerMail(ex);
		}
		return false;
	}
	
	

	/*
	 * private void flatJson(File[] files) { try { for (File file : files) { if
	 * (file.isDirectory()) { flatJson(file.listFiles()); } else {
	 * 
	 * if (file.getPath().endsWith(".json")) { SparkConf sparkConf = new SparkConf()
	 * .setAppName("OlympicMedals") .setMaster("local[*]")
	 * .set("spark.sql.crossJoin.enabled", "true")
	 * .set("spark.sql.shuffle.partitions", "1") .set("spark.sql.caseSensitive",
	 * "true") .set("spark.driver.memory", "571859200") .set("spark.testing.memory",
	 * "2147480000");
	 * 
	 * 
	 * 
	 * this.sparkSession = SparkSession.builder() .config(sparkConf) .getOrCreate();
	 * 
	 * 
	 * 
	 * Dataset<Row> dataFrame = sparkSession.read() .option("header", "true")
	 * .option("inferSchema", "true") //
	 * .json("///home//ueuser//Senthil//sample_data11.json"); .json(
	 * file.getAbsolutePath());
	 * 
	 * dataFrame = flattenJSONdf(dataFrame); dataFrame.cache();
	 * System.out.println("----------------faltted josn--------------------" +
	 * file.getAbsoluteFile().getParent());
	 * dataFrame.write().format("org.apache.spark.sql.json").mode(SaveMode.Overwrite
	 * ).save(file.getAbsoluteFile().getParent()+File.separator);
	 * 
	 * 
	 * 
	 * }
	 * 
	 * 
	 * } } } catch (Exception e) { e.printStackTrace(); StringWriter errors = new
	 * StringWriter(); e.printStackTrace(new PrintWriter(errors)); String ex =
	 * errors.toString(); ExceptionHandlerMail.errorTriggerMail(ex); } }
	 */
	

	
	public static void validateAndFormatJsonData(String filePath) { //remove double quotes in json data and string to numeric
		try {
			 if (filePath.endsWith(".json")) {
						Path path = Paths.get(filePath);
						Stream<String> lines = Files.lines(path);
						List<String> replaced = lines.map(line -> line.replaceAll("\"(?!0)(-?\\d+(?:[\\.,]\\d+)?)\"", "$1").replaceAll("\\\\\\\\\\\\\\\"","").replaceAll("\"0\"", "0")).collect(Collectors.toList());   //replaceAll("\"(-?\\d+(?:[\\.,]\\d+)?)\"", "$1")
						Files.write(path, replaced);
						lines.close();						
					}

				
			 
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
}
