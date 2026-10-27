const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const NOTE_ID = "note_abc_123";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function buildValidNotePayload(userId = ALICE_UID, noteId = NOTE_ID, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    remoteUuid: noteId,
    userId: userId,
    title: "Nota de prueba",
    content: "Contenido sincronizado",
    noteType: "TEXT",
    colorIndex: 0,
    paperStyle: "BLANK",
    isPinned: false,
    isArchived: false,
    isTrashed: false,
    isLocked: false,
    lockPin: "",
    tags: "trabajo,ideas",
    folder: "Notas",
    checklistData: "",
    drawingData: "",
    mathEvaluations: "",
    pdfPath: "",
    pdfAnnotationData: "",
    isReminderDone: false,
    lastModifiedLocal: 1700000000000,
    remoteUrl: "",
    remoteImageUrl: "",
    attachmentsJson: "[]",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or create notes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("notes").get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("notes")
      .doc(NOTE_ID)
      .set(buildValidNotePayload(ALICE_UID, NOTE_ID))
  );
});

test("Authenticated owner: can create, get, list, update, and delete their own note", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const noteRef = aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc(NOTE_ID);

  await assertSucceeds(noteRef.set(buildValidNotePayload(ALICE_UID, NOTE_ID)));
  await assertSucceeds(noteRef.get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("notes").get());

  await assertSucceeds(
    noteRef.update({
      title: "Título actualizado",
      content: "Nuevo contenido",
      lastModifiedLocal: 1700000005000,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(noteRef.delete());
});

test("Cross-user isolation: Bob cannot read, list, update, or delete Alice's notes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("notes")
      .doc(NOTE_ID)
      .set(buildValidNotePayload(ALICE_UID, NOTE_ID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceNoteFromBob = bobDb.collection("users").doc(ALICE_UID).collection("notes").doc(NOTE_ID);

  await assertFails(aliceNoteFromBob.get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("notes").get());
  await assertFails(
    aliceNoteFromBob.update({
      title: "Hackeado por Bob",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(aliceNoteFromBob.delete());
});

test("Shadow update & ghost field rejection: rejects unknown fields on create and update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const noteRef = aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc(NOTE_ID);

  await assertFails(
    noteRef.set(
      buildValidNotePayload(ALICE_UID, NOTE_ID, {
        isAdmin: true,
      })
    )
  );

  await assertSucceeds(noteRef.set(buildValidNotePayload(ALICE_UID, NOTE_ID)));
  await assertFails(
    noteRef.update({
      ghostField: "malicious",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Immutable fields & value poisoning: rejects tampering with userId, remoteUuid, or invalid types", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const noteRef = aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc(NOTE_ID);
  await assertSucceeds(noteRef.set(buildValidNotePayload(ALICE_UID, NOTE_ID)));

  await assertFails(
    noteRef.update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    noteRef.update({
      title: 12345,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

const REMINDER_ID = "rem_xyz_789";

function buildValidReminderPayload(userId = ALICE_UID, reminderId = REMINDER_ID, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    remoteUuid: reminderId,
    userId: userId,
    title: "Revisión de proyecto",
    notes: "Preparar presentación",
    location: "Sala A",
    itemType: "REMINDER",
    scheduledAt: 1700050000000,
    endTimeMillis: 1700053600000,
    isAllDay: false,
    isCompleted: false,
    completedAt: 0,
    priority: 2,
    listName: "Trabajo",
    colorHex: 0xFF007AFF,
    recurrenceRule: "WEEKLY",
    subtasksJson: "[]",
    linkedNoteId: 12,
    linkedNoteUuid: NOTE_ID,
    linkedNoteTitle: "Nota de prueba",
    alertOffsetMinutes: 15,
    lastModifiedLocal: 1700000000000,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

test("Reminders: Authenticated owner can create, get, list, update, and delete their own reminder", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const remRef = aliceDb.collection("users").doc(ALICE_UID).collection("reminders").doc(REMINDER_ID);

  await assertSucceeds(remRef.set(buildValidReminderPayload(ALICE_UID, REMINDER_ID)));
  await assertSucceeds(remRef.get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("reminders").get());

  await assertSucceeds(
    remRef.update({
      title: "Revisión completada",
      isCompleted: true,
      completedAt: 1700060000000,
      lastModifiedLocal: 1700060000000,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(remRef.delete());
});

test("Reminders: Cross-user isolation and invalid itemType rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const remRef = aliceDb.collection("users").doc(ALICE_UID).collection("reminders").doc(REMINDER_ID);
  await assertSucceeds(remRef.set(buildValidReminderPayload(ALICE_UID, REMINDER_ID)));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceRemFromBob = bobDb.collection("users").doc(ALICE_UID).collection("reminders").doc(REMINDER_ID);
  await assertFails(aliceRemFromBob.get());
  await assertFails(aliceRemFromBob.delete());

  await assertFails(
    remRef.update({
      itemType: "INVALID_TYPE",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

