package com.zenfra.queries;

import lombok.Data;

@Data
public class FavouriteViewQueries {

    
	private String getFavView;
	
	private String save;
	
	private String updateCreatedByEqualsUserId;
	
	private String updateCreatedByNotEqualsUserIdUserAccessUpdate;
	private String updateCreatedByNotEqualsUserIdUserRemoveUpdate;
	
	private String selectByFavouriteId;
	
	

	public String getGetFavView() {
		return getFavView;
	}

	public void setGetFavView(String getFavView) {
		this.getFavView = getFavView;
	}

	public String getSave() {
		return save;
	}

	public void setSave(String save) {
		this.save = save;
	}

	public String getUpdateCreatedByEqualsUserId() {
		return updateCreatedByEqualsUserId;
	}

	public void setUpdateCreatedByEqualsUserId(String updateCreatedByEqualsUserId) {
		this.updateCreatedByEqualsUserId = updateCreatedByEqualsUserId;
	}

	
	

	public String getUpdateCreatedByNotEqualsUserIdUserAccessUpdate() {
		return updateCreatedByNotEqualsUserIdUserAccessUpdate;
	}

	public void setUpdateCreatedByNotEqualsUserIdUserAccessUpdate(String updateCreatedByNotEqualsUserIdUserAccessUpdate) {
		this.updateCreatedByNotEqualsUserIdUserAccessUpdate = updateCreatedByNotEqualsUserIdUserAccessUpdate;
	}

	public String getUpdateCreatedByNotEqualsUserIdUserRemoveUpdate() {
		return updateCreatedByNotEqualsUserIdUserRemoveUpdate;
	}

	public void setUpdateCreatedByNotEqualsUserIdUserRemoveUpdate(String updateCreatedByNotEqualsUserIdUserRemoveUpdate) {
		this.updateCreatedByNotEqualsUserIdUserRemoveUpdate = updateCreatedByNotEqualsUserIdUserRemoveUpdate;
	}

	public String getSelectByFavouriteId() {
		return selectByFavouriteId;
	}

	public void setSelectByFavouriteId(String selectByFavouriteId) {
		this.selectByFavouriteId = selectByFavouriteId;
	}
	
	
}
