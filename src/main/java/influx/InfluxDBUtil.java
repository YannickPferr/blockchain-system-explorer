package influx;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBException;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult.Result;
import org.influxdb.dto.QueryResult.Series;

public class InfluxDBUtil {

	/**
	 * Exports Data from InfluxDB into a csv file
	 * 
	 * @param influxDB
	 *            - An instance of a {@link InfluxDB}
	 * @param query
	 *            - The Influx query to extract data with
	 */
	public static void exportData(InfluxDB influxDB, String query) {
		List<Result> results = null;
		try {
			results = influxDB.query(new Query(query)).getResults();
		} catch (InfluxDBException e) {
			System.err.println("Malformed Query!");
			return;
		}

		if (results != null && results.get(0).getSeries() != null) {
			// Field order: time, BlockNumber, Client, Hash, Orphan, Value
			StringBuilder csv = new StringBuilder();
			List<Series> seriesL = results.get(0).getSeries();
			// set csv header
			for (int j = 0; j < seriesL.size(); j++) {
				Series series = seriesL.get(j);
				// write header
				for (String column : series.getColumns())
					csv.append(column + ",");
				// Delete last comma
				csv.deleteCharAt(csv.length() - 1);
				// next line
				csv.append(System.lineSeparator());

				// write values
				for (List<Object> row : series.getValues()) {
					// print list and delete brackets
					String fieldValues = row.toString().substring(1, row.toString().length() - 1);
					csv.append(fieldValues + System.lineSeparator());
				}

				try {
					SimpleDateFormat format = new SimpleDateFormat("HHmmddMMyyyy");
					// Create exports folder if not exists
					File exportsFolder = new File("exports");
					exportsFolder.mkdir();
					FileWriter fw = new FileWriter(new File(exportsFolder.getName() + File.separator + series.getName()
							+ "-" + format.format(new Date()) + ".csv"));
					fw.write(csv.toString());
					fw.close();
					System.out.println("CSV file for series " + series.getName() + " successfully created!");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.err.println("Error when trying to write CSV file for series " + series.getName() + "!");
					e.printStackTrace();
				}
			}
		} else
			System.out.println("Query returned no results!");
	}

	/**
	 * Connects to an Influx database
	 * 
	 * @param influxDB
	 *            - An instance of a {@link InfluxDB}
	 * @param dbName
	 *            - The database name to connect to
	 * @return <code>true</code> if connection was successful otherwise
	 *         <code>false</code>
	 */
	public static boolean connectToDB(InfluxDB influxDB, String dbName) {
		// TODO Auto-generated method stub
		try {
			influxDB.query(new Query("CREATE DATABASE " + dbName));
		} catch (org.influxdb.InfluxDBIOException e) {
			return false;
		}

		influxDB.setDatabase(dbName);
		return true;
	}
}
