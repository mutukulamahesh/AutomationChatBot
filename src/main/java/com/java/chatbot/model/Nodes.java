package com.java.chatbot.model;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * @author ${Mahesh M Kumar}
 * @date Jun 19, 2018
 */

@Entity
@EntityListeners(AuditingEntityListener.class)
public class Nodes {

	@Id
	private int id;
	private String node;
	private int allowCustom;

	public Nodes() {}

	public Nodes(String node, int allowCustom){
		this.node = node;
		this.allowCustom = allowCustom;
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getNode() {
		return node;
	}
	public void setNode(String node) {
		this.node = node;
	}
	public int getAllowCustom() { return allowCustom; }
	public void setAllowCustom(int allowCustom) { this.allowCustom = allowCustom; }
}
