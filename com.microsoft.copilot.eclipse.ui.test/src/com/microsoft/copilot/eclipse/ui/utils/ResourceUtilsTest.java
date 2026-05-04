// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
	
	@Mock
	private IProject mockProjectA;
	
	@Mock
	private IProject mockProjectB;

	private MockedStatic<FileUtils> mockedFileUtils;
	private MockedStatic<LSPEclipseUtils> mockedLspUtils;
	
	private static final String nameProjectA= "ProjectA";
	private static final String nameProjectB= "ProjectB";

	@BeforeEach
	void setUp() {
		mockedFileUtils = mockStatic(FileUtils.class);
		mockedFileUtils.when(() -> FileUtils.isExcludedFromReferencedFiles(mockValidFile)).thenReturn(false);
		mockedFileUtils.when(() -> FileUtils.isExcludedFromReferencedFiles(mockInvalidFile)).thenReturn(true);

		mockedLspUtils = mockStatic(LSPEclipseUtils.class);
		mockedLspUtils.when(() -> LSPEclipseUtils.toWorkspaceFolder(mockProjectA))
			.thenReturn(new WorkspaceFolder("file:///" + nameProjectA, nameProjectA));
		mockedLspUtils.when(() -> LSPEclipseUtils.toWorkspaceFolder(mockProjectB))
			.thenReturn(new WorkspaceFolder("file:///" + nameProjectB, nameProjectB));
	}

	@AfterEach
	void tearDown() {
		if (mockedFileUtils != null) {
			mockedFileUtils.close();
		}
		if (mockedLspUtils != null) {
			mockedLspUtils.close();
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

	private static Stream<List<IResource>> provideResourcesForNeverNullTest() {
		return Stream.of(
				null,
				List.of(),
				Arrays.asList((IResource) null),
				List.of(mock(IFolder.class)),
				List.of(mock(IProject.class))
		);
	}

	@ParameterizedTest
	@MethodSource("provideResourcesForNeverNullTest")
	void testDeriveWorkspaceFoldersReturnsNeverNull(List<IResource> resources) {
		List<WorkspaceFolder> result = ResourceUtils.deriveWorkspaceFoldersFrom(resources);

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void testDeriveWorkspaceFoldersWithMultipleResources() {
		when(mockValidFile.getProject()).thenReturn(mockProjectA);
		when(mockFolder.getProject()).thenReturn(mockProjectB);
		when(mockProjectA.isAccessible()).thenReturn(true);
		when(mockProjectB.isAccessible()).thenReturn(true);

		List<WorkspaceFolder> result = ResourceUtils.deriveWorkspaceFoldersFrom(List.of(mockValidFile, mockFolder));

		assertNotNull(result);
		assertFalse(result.isEmpty());
		assertEquals(2, result.size(), "Both projects from both resources should be derived as workspace folders");
	}

	@Test
	void testDeriveWorkspaceFoldersDoesNotReturnDuplicates() {
		when(mockValidFile.getProject()).thenReturn(mockProjectA);
		when(mockFolder.getProject()).thenReturn(mockProjectA);
		when(mockProjectA.isAccessible()).thenReturn(true);
		when(mockProjectA.getName()).thenReturn(nameProjectA);

		List<WorkspaceFolder> result = ResourceUtils.deriveWorkspaceFoldersFrom(List.of(mockValidFile, mockFolder));

		assertNotNull(result);
		assertFalse(result.isEmpty());
		assertEquals(1, result.size(), "Projects in derived workspaces folders should be unique, no duplicates.");
		assertEquals(mockProjectA.getName(), result.get(0).getName());
	}
}
