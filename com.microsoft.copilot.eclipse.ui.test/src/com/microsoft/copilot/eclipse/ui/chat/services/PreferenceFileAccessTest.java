// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.

package com.microsoft.copilot.eclipse.ui.chat.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PreferenceFileAccessTest {
  private Path directory;

  @BeforeEach
  void setUp() throws Exception {
    directory = Files.createDirectories(Path.of("target", "preference-file-tests", UUID.randomUUID().toString()))
        .toAbsolutePath();
  }

  @AfterEach
  void tearDown() throws Exception {
    try (var paths = Files.walk(directory)) {
      for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
        Files.delete(path);
      }
    }
  }

  @Test
  void testWrite_ExistingPreferences_ReplacesCompleteUtf8Document() throws Exception {
    Path target = createPreferences("{\"chatModeName\":\"Ask\"}");
    PreferenceStorage.FileAccess files = new PreferenceFileAccess();

    files.write(target, "{\"chatModeName\":\"Agent\",\"userInputs\":[\"你好\"]}");

    assertEquals("{\"chatModeName\":\"Agent\",\"userInputs\":[\"你好\"]}", files.read(target));
    assertDirectoryContents(target);
  }

  @Test
  void testRead_MissingFile_PropagatesMissingOutcomeWithoutCreatingFile() throws Exception {
    Path target = directory.resolve("pref.json");
    PreferenceStorage.FileAccess files = new PreferenceFileAccess();

    assertThrows(NoSuchFileException.class, () -> files.read(target));

    assertDirectoryContents();
  }

  @Test
  void testRead_NonFile_PropagatesFailureWithoutChangingTarget() throws Exception {
    Path target = Files.createDirectory(directory.resolve("pref.json"));
    PreferenceStorage.FileAccess files = new PreferenceFileAccess();

    assertThrows(IOException.class, () -> files.read(target));

    assertDirectoryContents(target);
  }

  @Test
  void testRead_AclDenied_PropagatesFailureWithoutChangingTarget() throws Exception {
    Path target = Files.writeString(directory.resolve("pref.json"), "{\"chatModeName\":\"Ask\"}");
    AclFileAttributeView view = Files.getFileAttributeView(target, AclFileAttributeView.class);
    assumeTrue(view != null, "ACL protections require an ACL-capable file system");
    var original = view.getAcl();
    var permissions = EnumSet.allOf(AclEntryPermission.class);
    permissions.remove(AclEntryPermission.READ_DATA);
    PreferenceStorage.FileAccess files = new PreferenceFileAccess();
    try {
      view.setAcl(List.of(AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(view.getOwner())
          .setPermissions(permissions).build()));

      assertThrows(AccessDeniedException.class, () -> files.read(target));
    } finally {
      view.setAcl(original);
    }

    assertEquals("{\"chatModeName\":\"Ask\"}", files.read(target));
    assertDirectoryContents(target);
  }

  @Test
  void testRead_PosixDenied_PropagatesFailureWithoutChangingTarget() throws Exception {
    Path target = Files.writeString(directory.resolve("pref.json"), "{\"chatModeName\":\"Ask\"}");
    PosixFileAttributeView view = Files.getFileAttributeView(target, PosixFileAttributeView.class);
    assumeTrue(view != null, "POSIX protections require a POSIX file system");
    var original = view.readAttributes().permissions();
    PreferenceStorage.FileAccess files = new PreferenceFileAccess();
    try {
      view.setPermissions(PosixFilePermissions.fromString("---------"));
      assumeTrue(!Files.isReadable(target), "Privileged users may bypass POSIX read restrictions");

      assertThrows(AccessDeniedException.class, () -> files.read(target));
    } finally {
      view.setPermissions(original);
    }

    assertEquals("{\"chatModeName\":\"Ask\"}", files.read(target));
    assertDirectoryContents(target);
  }

  @Test
  void testWrite_FirstSave_CreatesAccountDirectoryAndCompleteFile() throws Exception {
    Path target = directory.resolve("alice").resolve("pref.json");
    PreferenceStorage.FileAccess files = new PreferenceFileAccess();

    files.write(target, "{\"chatModel\":\"model-a\"}");

    assertEquals("{\"chatModel\":\"model-a\"}", files.read(target));
    try (var children = Files.list(target.getParent())) {
      assertEquals(List.of(target), children.toList());
    }
  }

  @Test
  void testWrite_PartialSnapshot_ReadersKeepPreviousCompleteDocumentUntilReplacement() throws Exception {
    Path target = createPreferences("{\"chatModeName\":\"Ask\"}");
    String snapshot = "{\"chatModeName\":\"Agent\"}";
    PreferenceStorage.FileAccess reader = new PreferenceFileAccess();
    PreferenceStorage.FileAccess files = new PreferenceFileAccess((temporary, content) -> {
      assertEquals(target.getParent(), temporary.getParent());
      assertNotEquals(target, temporary);
      Files.writeString(temporary, "{\"chatModeName\":");
      assertEquals("{\"chatModeName\":\"Ask\"}", reader.read(target));
      Files.writeString(temporary, content);
    }, (temporary, destination) -> {
      assertEquals(snapshot, reader.read(temporary));
      assertEquals("{\"chatModeName\":\"Ask\"}", reader.read(destination));
      replaceAtomically(temporary, destination);
    });

    files.write(target, snapshot);

    assertEquals(snapshot, reader.read(target));
    assertDirectoryContents(target);
  }

  @Test
  void testWrite_PartialWriteFailure_PreservesOriginalAndCleansOnlyOwnedFile() throws Exception {
    Path target = createPreferences("{\"chatModeName\":\"Ask\"}");
    Path otherWriter = Files.writeString(directory.resolve(".copilot-preferences-other.tmp"), "another writer");
    IOException failure = new IOException("Injected disk-full failure");
    PreferenceStorage.FileAccess files = new PreferenceFileAccess((temporary, content) -> {
      Files.writeString(temporary, "{\"chatModeName\":");
      throw failure;
    }, PreferenceFileAccessTest::replaceAtomically);

    assertSame(failure, assertThrows(IOException.class, () -> files.write(target, "{\"chatModeName\":\"Agent\"}")));

    assertEquals("{\"chatModeName\":\"Ask\"}", files.read(target));
    assertEquals("another writer", Files.readString(otherWriter));
    assertDirectoryContents(target, otherWriter);
  }

  @Test
  void testWrite_ReplacementFailure_PreservesOriginalAndCleansOnlyOwnedFile() throws Exception {
    Path target = createPreferences("{\"chatModeName\":\"Ask\"}");
    Path otherWriter = Files.writeString(directory.resolve(".copilot-preferences-other.tmp"), "another writer");
    IOException failure = new IOException("Injected replacement failure");
    PreferenceStorage.FileAccess files = new PreferenceFileAccess(Files::writeString, (temporary, destination) -> {
      assertEquals("{\"chatModeName\":\"Agent\"}", Files.readString(temporary));
      throw failure;
    });

    assertSame(failure, assertThrows(IOException.class, () -> files.write(target, "{\"chatModeName\":\"Agent\"}")));

    assertEquals("{\"chatModeName\":\"Ask\"}", files.read(target));
    assertEquals("another writer", Files.readString(otherWriter));
    assertDirectoryContents(target, otherWriter);
  }

  @Test
  void testWrite_AtomicMoveUnsupported_FailsWithoutTruncatingFallback() throws Exception {
    Path target = createPreferences("{\"chatModeName\":\"Ask\"}");
    PreferenceStorage.FileAccess files = new PreferenceFileAccess(Files::writeString, (temporary, destination) -> {
      throw new AtomicMoveNotSupportedException(temporary.toString(), destination.toString(), "Unsupported");
    });

    assertThrows(AtomicMoveNotSupportedException.class, () -> files.write(target, "{\"chatModeName\":\"Agent\"}"));

    assertEquals("{\"chatModeName\":\"Ask\"}", files.read(target));
    assertDirectoryContents(target);
  }

  @Test
  void testWrite_FirstSaveFailure_DoesNotPublishPartialFile() throws Exception {
    Path target = directory.resolve("pref.json");
    PreferenceStorage.FileAccess files = new PreferenceFileAccess((temporary, content) -> {
      Files.writeString(temporary, "{");
      throw new IOException("Injected write failure");
    }, PreferenceFileAccessTest::replaceAtomically);

    assertThrows(IOException.class, () -> files.write(target, "{\"chatModeName\":\"Agent\"}"));

    assertFalse(Files.exists(target));
    assertDirectoryContents();
  }

  @Test
  void testWrite_OverlappingWriters_UseDistinctOwnedFilesAndLastCompletedSaveWins() throws Exception {
    Path target = createPreferences("{\"chatModeName\":\"Ask\"}");
    AtomicReference<Path> firstTemporary = new AtomicReference<>();
    PreferenceStorage.FileAccess second = new PreferenceFileAccess((temporary, content) -> {
      assertNotEquals(firstTemporary.get(), temporary);
      Files.writeString(temporary, content);
    }, PreferenceFileAccessTest::replaceAtomically);
    PreferenceStorage.FileAccess first = new PreferenceFileAccess((temporary, content) -> {
      firstTemporary.set(temporary);
      Files.writeString(temporary, "{");
      second.write(target, "{\"chatModeName\":\"Second\"}");
      assertEquals("{\"chatModeName\":\"Second\"}", second.read(target));
      assertEquals("{", Files.readString(temporary));
      Files.writeString(temporary, content);
    }, PreferenceFileAccessTest::replaceAtomically);

    first.write(target, "{\"chatModeName\":\"First\"}");

    assertEquals("{\"chatModeName\":\"First\"}", first.read(target));
    assertDirectoryContents(target);
  }

  @Test
  void testWrite_PosixTarget_PreservesOwnerPermissionsOwnerAndGroupBeforeWriting() throws Exception {
    Path target = Files.writeString(directory.resolve("pref.json"), "{}");
    PosixFileAttributeView view = Files.getFileAttributeView(target, PosixFileAttributeView.class);
    assumeTrue(view != null, "POSIX protections require a POSIX file system");
    view.setPermissions(PosixFilePermissions.fromString("rwxr-----"));
    var original = view.readAttributes();
    var expectedPermissions = Files.getFileAttributeView(target, AclFileAttributeView.class) == null
        ? PosixFilePermissions.fromString("rwx------") : original.permissions();
    PreferenceStorage.FileAccess files = new PreferenceFileAccess((temporary, content) -> {
      var attributes = Files.readAttributes(temporary, PosixFileAttributes.class);
      assertEquals(expectedPermissions, attributes.permissions());
      assertEquals(original.owner(), attributes.owner());
      assertEquals(original.group(), attributes.group());
      Files.writeString(temporary, content);
    }, PreferenceFileAccessTest::replaceAtomically);

    files.write(target, "{\"chatModeName\":\"Agent\"}");

    var restored = view.readAttributes();
    assertEquals(expectedPermissions, restored.permissions());
    assertEquals(original.owner(), restored.owner());
    assertEquals(original.group(), restored.group());
    assertEquals("{\"chatModeName\":\"Agent\"}", files.read(target));
    assertDirectoryContents(target);
  }

  @ParameterizedTest
  @ValueSource(strings = {"rw-r-----", "rw----r--", "rw-r--r--", "rw--w----", "rw---x---"})
  void testWrite_PosixUnexposedAcl_NarrowsSharingAndPreservesOwnerAndGroup(String permissions) throws Exception {
    Path target = Files.writeString(directory.resolve("pref.json"), "{\"chatModeName\":\"Ask\"}");
    PosixFileAttributeView view = Files.getFileAttributeView(target, PosixFileAttributeView.class);
    assumeTrue(view != null && Files.getFileAttributeView(target, AclFileAttributeView.class) == null,
        "Provider does not expose POSIX ACLs");
    view.setPermissions(PosixFilePermissions.fromString(permissions));
    var original = view.readAttributes();
    PreferenceStorage.FileAccess files = new PreferenceFileAccess();

    files.write(target, "{\"chatModeName\":\"Agent\"}");

    assertEquals("{\"chatModeName\":\"Agent\"}", files.read(target));
    assertEquals(PosixFilePermissions.fromString("rw-------"), view.readAttributes().permissions());
    assertEquals(original.owner(), view.readAttributes().owner());
    assertEquals(original.group(), view.readAttributes().group());
    assertDirectoryContents(target);
  }

  @Test
  void testWrite_PosixReplacementFailure_PreservesOriginalSharingAndContents() throws Exception {
    Path target = Files.writeString(directory.resolve("pref.json"), "{\"chatModeName\":\"Ask\"}");
    PosixFileAttributeView view = Files.getFileAttributeView(target, PosixFileAttributeView.class);
    assumeTrue(view != null && Files.getFileAttributeView(target, AclFileAttributeView.class) == null,
        "Provider does not expose POSIX ACLs");
    view.setPermissions(PosixFilePermissions.fromString("rw-r-----"));
    var original = view.readAttributes();
    IOException failure = new IOException("Injected replacement failure");
    PreferenceStorage.FileAccess files = new PreferenceFileAccess(Files::writeString, (temporary, destination) -> {
      assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(temporary));
      throw failure;
    });

    assertSame(failure, assertThrows(IOException.class, () -> files.write(target, "{\"chatModeName\":\"Agent\"}")));

    assertEquals("{\"chatModeName\":\"Ask\"}", files.read(target));
    assertEquals(original.permissions(), view.readAttributes().permissions());
    assertEquals(original.owner(), view.readAttributes().owner());
    assertEquals(original.group(), view.readAttributes().group());
    assertDirectoryContents(target);
  }

  @Test
  void testWrite_AclTarget_PreservesAclAndOwnerBeforeWriting() throws Exception {
    Path target = Files.writeString(directory.resolve("pref.json"), "{}");
    AclFileAttributeView view = Files.getFileAttributeView(target, AclFileAttributeView.class);
    assumeTrue(view != null, "ACL protections require an ACL-capable file system");
    var owner = view.getOwner();
    view.setAcl(List.of(AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(owner)
        .setPermissions(EnumSet.allOf(AclEntryPermission.class)).build()));
    var original = view.getAcl();
    PreferenceStorage.FileAccess files = new PreferenceFileAccess((temporary, content) -> {
      var protections = Files.getFileAttributeView(temporary, AclFileAttributeView.class);
      assertEquals(original, protections.getAcl());
      assertEquals(owner, protections.getOwner());
      Files.writeString(temporary, content);
    }, PreferenceFileAccessTest::replaceAtomically);

    files.write(target, "{\"chatModeName\":\"Agent\"}");

    assertEquals(original, view.getAcl());
    assertEquals(owner, view.getOwner());
    assertEquals("{\"chatModeName\":\"Agent\"}", files.read(target));
    assertDirectoryContents(target);
  }

  @Test
  void testWrite_NonRegularTarget_FailsWithoutRemovingTargetOrOtherFiles() throws Exception {
    Path target = Files.createDirectory(directory.resolve("pref.json"));
    PreferenceStorage.FileAccess files = new PreferenceFileAccess();

    assertThrows(IOException.class, () -> files.write(target, "{}"));

    assertDirectoryContents(target);
  }

  private static void replaceAtomically(Path source, Path target) throws IOException {
    Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
  }

  private Path createPreferences(String content) throws IOException {
    Path target = Files.writeString(directory.resolve("pref.json"), content);
    PosixFileAttributeView posix = Files.getFileAttributeView(target, PosixFileAttributeView.class);
    if (posix != null) {
      posix.setPermissions(PosixFilePermissions.fromString("rw-------"));
    }
    return target;
  }

  private void assertDirectoryContents(Path... expected) throws IOException {
    try (var children = Files.list(directory)) {
      assertEquals(List.of(expected).stream().sorted().toList(), children.sorted().toList());
    }
  }
}
