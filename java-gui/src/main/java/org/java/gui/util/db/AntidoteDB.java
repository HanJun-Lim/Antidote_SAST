package org.java.gui.util.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AntidoteDB {
	// db 생성과 접근을 위한 루틴
	private final String dbName = "Antidote_statistics.db";
	private String currentPath;
	private Connection conn;
	private PreparedStatement pstmt;
	private ResultSet rs;

	// 초기 드라이버 한번 로드
	static {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public AntidoteDB() {
		this.currentPath = System.getProperty("user.dir") + "/";
	}

	// 데이터베이스 연결 메소드
	public boolean dbConnect() {
		try {
			conn = DriverManager.getConnection("jdbc:sqlite:" + currentPath + dbName); // jdbc:sqlite:폴더명/파일이름

		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			return false;
		}
		return true;
	}

	// UPDATE, INSERT, DELETE 쿼리 수행
	public boolean setQuery(String query, List<Object> data) {
		try {
			int i = 0;
			pstmt = conn.prepareStatement(query);
			for (Object elem : data) {
				i++;
				if (elem instanceof Integer) {
					pstmt.setInt(i, (int) elem);
				} else if (elem instanceof String) {
					pstmt.setString(i, (String) elem);
				} else {
					return false;
				}
			}
			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return true;
	}

	// SELECT 특정 레코드 받아오기
	public ArrayList<Object[]> getSearchTotalStatistics() {
		ArrayList<Object[]> returnVal = new ArrayList<>();

		String query = "SELECT * FROM total_statistics ORDER BY date_time DESC LIMIT 30"; // 오름차순은 ASC
		try {
			pstmt = conn.prepareStatement(query);

			rs = pstmt.executeQuery();

			while (rs.next()) {

				Object[] dbReturnVal = new Object[2];

				dbReturnVal[0] = rs.getString("date_time");
				dbReturnVal[1] = rs.getInt("compliant_per");

				returnVal.add(dbReturnVal);
			}

			rs.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return returnVal;
	}

	public ArrayList<Object[]> getSearchDetailedStatistics(String date_time) {
		ArrayList<Object[]> returnVal = new ArrayList<>();

		String query = "SELECT * FROM detailed_statistics WHERE date_time='" + date_time + "'";
		try {
			pstmt = conn.prepareStatement(query);

			rs = pstmt.executeQuery();

			while (rs.next()) {

				Object[] dbReturnVal = new Object[4];

				dbReturnVal[0] = rs.getString("date_time");
				dbReturnVal[1] = rs.getString("filepath");
				dbReturnVal[2] = rs.getString("rulekey");
				dbReturnVal[3] = rs.getInt("count");

				returnVal.add(dbReturnVal);
			}

			rs.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return returnVal;
	}

	public int getSearchDetailedCount(String date_time, String filePath, String ruleKey) {
		int returnVal = 0;

		String query = "SELECT count FROM detailed_statistics WHERE filePath='" + filePath + "' AND ruleKey='" + ruleKey
				+ "' AND date_time='" + date_time + "'";
		try {
			pstmt = conn.prepareStatement(query);

			rs = pstmt.executeQuery();

			while (rs.next()) {

				returnVal = rs.getInt("count");
			}

			rs.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return returnVal;
	}
	
	public void close() {
		
		if(this.rs != null) try {this.rs.close();}catch(Exception e) {}
		if(this.pstmt != null) try {this.pstmt.close();}catch(Exception e) {}
		if(this.conn != null) try {this.conn.close();}catch(Exception e) {}
	}

}