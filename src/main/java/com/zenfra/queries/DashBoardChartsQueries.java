package com.zenfra.queries;

import lombok.Data;

@Data
public class DashBoardChartsQueries {

	private String delete;

	public String getDelete() {
		return delete;
	}

	public void setDelete(String delete) {
		this.delete = delete;
	}

}
