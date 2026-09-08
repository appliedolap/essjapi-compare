/*
 * Copyright 2017-2026 Applied OLAP, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

	private final Version previousVersion;

	private final Version currentVersion;

	/** Filled in after construction: the next version is not known until the one after is read. */
	private Version nextVersion;

	private final List<JApiClass> changes;

	/**
	 * Records the outcome of comparing two jars.
	 *
	 * @param currentVersion the newer of the two jars compared
	 * @param previousVersion the older of the two
	 * @param changes every class japicmp had something to say about
	 */
	public ChangeSet(Version currentVersion, Version previousVersion, List<JApiClass> changes) {
		this.currentVersion = currentVersion;
		this.previousVersion = previousVersion;
		this.changes = changes;
	}

	/** The version this one is compared against. */
	public Version getPreviousVersion() {
		return previousVersion;
	}

	/** The version this change set describes. */
	public Version getCurrentVersion() {
		return currentVersion;
	}

	/** The version after this one, or null for the newest jar in the comparison. */
	public Version getNextVersion() {
		return nextVersion;
	}

	/**
	 * Records the version that follows this one, so the report can link forward as well as back.
	 *
	 * @param nextVersion the following version, or null if this is the newest
	 */
	public void setNextVersion(Version nextVersion) {
		this.nextVersion = nextVersion;
	}

	/** Every class japicmp reported on, whatever happened to it. */
	public List<JApiClass> getChanges() {
		return changes;
	}

	/*
	 * The four views of this change set that the report actually renders. The template used to
	 * iterate getChanges() four times over and discard everything that did not belong in the
	 * section it was building, which left Thymeleaf emitting the indentation of every element it
	 * had removed - see ApiChanges.
	 */

	/** Classes that appear in this version and not the previous one. */
	public List<JApiClass> getAddedClasses() {
		return ApiChanges.withStatus(changes, JApiChangeStatus.NEW);
	}

	/** Classes present in both versions whose members changed. */
	public List<JApiClass> getModifiedClasses() {
		return ApiChanges.withStatus(changes, JApiChangeStatus.MODIFIED);
	}

	/** Classes present in the previous version and gone from this one. */
	public List<JApiClass> getRemovedClasses() {
		return ApiChanges.withStatus(changes, JApiChangeStatus.REMOVED);
	}

	/** Classes that gained at least one deprecated method in this version. */
	public List<JApiClass> getClassesWithNewDeprecations() {
		return ApiChanges.withNewDeprecations(changes);
	}

	/**
	 * Whether this comparison found nothing to report. Most Essbase patch releases change no
	 * public API at all, so around half of these say nothing; the report states that once rather
	 * than showing four empty headings.
	 *
	 * @return true if nothing at all changed between these two versions
	 */
	public boolean getNoChanges() {
		return getAddedClasses().isEmpty()
				&& getModifiedClasses().isEmpty()
				&& getRemovedClasses().isEmpty()
				&& getClassesWithNewDeprecations().isEmpty();
	}

}
