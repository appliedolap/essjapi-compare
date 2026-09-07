package com.appliedolap.essjapicompare;

import java.util.List;

import japicmp.model.JApiChangeStatus;
import japicmp.model.JApiClass;

/**
 * Helper object that represents the actual thing being iterated in the HTML
 * output. Mostly just a container for the List of JApiClass changes, but also
 * contains some other useful info.
 * 
 * @author jasonwjones
 *
 */
public class ChangeSet {

	private Version previousVersion;

	private Version currentVersion;

	private Version nextVersion;

	private List<JApiClass> changes;

	public ChangeSet(Version currentVersion, Version previousVersion, List<JApiClass> changes) {
		this.currentVersion = currentVersion;
		this.previousVersion = previousVersion;
		this.changes = changes;
	}

	public Version getPreviousVersion() {
		return previousVersion;
	}

	public void setPreviousVersion(Version previousVersion) {
		this.previousVersion = previousVersion;
	}

	public Version getCurrentVersion() {
		return currentVersion;
	}

	public void setCurrentVersion(Version currentVersion) {
		this.currentVersion = currentVersion;
	}

	public Version getNextVersion() {
		return nextVersion;
	}

	public void setNextVersion(Version nextVersion) {
		this.nextVersion = nextVersion;
	}

	public List<JApiClass> getChanges() {
		return changes;
	}

	/*
	 * The four views of this change set that the report actually renders. The template used to
	 * iterate getChanges() four times over and discard everything that did not belong in the
	 * section it was building, which left Thymeleaf emitting the indentation of every element it
	 * had removed - see ApiChanges.
	 */

	public List<JApiClass> getAddedClasses() {
		return ApiChanges.withStatus(changes, JApiChangeStatus.NEW);
	}

	public List<JApiClass> getModifiedClasses() {
		return ApiChanges.withStatus(changes, JApiChangeStatus.MODIFIED);
	}

	public List<JApiClass> getRemovedClasses() {
		return ApiChanges.withStatus(changes, JApiChangeStatus.REMOVED);
	}

	public List<JApiClass> getClassesWithNewDeprecations() {
		return ApiChanges.withNewDeprecations(changes);
	}

	public void setChanges(List<JApiClass> changes) {
		this.changes = changes;
	}

}
