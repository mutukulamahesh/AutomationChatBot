package com.java.chatbot.model;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * @author ${Mahesh M Kumar}
 * @date Jun 19, 2018
 */

@Entity
@EntityListeners(AuditingEntityListener.class)
public class Relations {

	@Id
	private int id;
	private int parentNodeId;
    private int childNodeId;
    private int vocabularyId;
	private int hadNextRelations;
	
	public Relations() {}

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getParentNodeId() {
		return parentNodeId;
	}
	public void setParentNodeId(int parentNodeId) {
		this.parentNodeId = parentNodeId;
	}

	public int getHadNextRelations() {
		return hadNextRelations;
	}

	public void setHadNextRelations(int hadNextRelations) {
		this.hadNextRelations = hadNextRelations;
	}

    public int getChildNodeId() {
        return childNodeId;
    }

    public void setChildNodeId(int childNodeId) {
        this.childNodeId = childNodeId;
    }

    public int getVocabularyId() {
        return vocabularyId;
    }

    public void setVocabularyId(int vocabularyId) {
        this.vocabularyId = vocabularyId;
    }
}
