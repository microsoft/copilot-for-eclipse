// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.viewers.StructuredSelection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.copilot.eclipse.core.utils.FileUtils;

/**
 * Tests for ResourceUtils core functionality
 */
@ExtendWith(MockitoExtension.class)
class ResourceUtilsTest {

	@Mock
	private IFile mockValidFile;

	@Mock
	private IFile mockInvalidFile;

	@Mock
	private IFolder mockFolder;

	private MockedStatic<FileUtils> mockedFileUtils;

	@BeforeEach
	void setUp() {
		mockedFileUtils = mockStatic(FileUtils.class);
		mockedFileUtils.when(() -> FileUtils.isExcludedFromReferencedFiles(mockValidFile)).thenReturn(false);
		mockedFileUtils.when(() -> FileUtils.isExcludedFromReferencedFiles(mockInvalidFile)).thenReturn(true);
	}

	@AfterEach
	void tearDown() {
		if (mockedFileUtils != null) {
			mockedFileUtils.close();
		}
	}

	@Test
	void testComplexMixedSelectionWithMocks() {
		var complexSelection = new StructuredSelection(Arrays.asList(mockValidFile, mockInvalidFile, mockFolder,
				"just a string", Integer.valueOf(42), new Object()));

		ResourceUtils.SelectionStats stats = ResourceUtils.analyzeSelection(complexSelection);

		assertEquals(1, stats.fileCount, "Should have 1 valid file");
		assertEquals(1, stats.folderCount, "Should have 1 folder");
		assertEquals(4, stats.invalidCount,
				"Should have 4 invalid objects (excluded file + string + number + Object)");

		assertFalse(stats.hasOnlyFiles(), "Should not have only files (has folders and invalid objects)");
		assertFalse(stats.hasOnlyFolders(), "Should not have only folders (has files and invalid objects)");
		assertFalse(stats.hasOnlyValidResources(), "Should not have only valid resources (has invalid objects)");
	}

	@Test
	void testCollectValidResourcesWithMocks() {
		var mixedSelection = new StructuredSelection(
				Arrays.asList(mockValidFile, mockInvalidFile, mockFolder, "invalid object"));

		var validResources = ResourceUtils.collectValidResources(mixedSelection);

		assertNotNull(validResources);
		assertEquals(2, validResources.size(), "Should contain valid file and folder");
		assertTrue(validResources.contains(mockValidFile), "Should contain valid file");
		assertTrue(validResources.contains(mockFolder), "Should contain folder");
		assertFalse(validResources.contains(mockInvalidFile), "Should not contain excluded file");
	}
}
