# Frontend

## New

### `DUUIWeb/src/routes/processes/+page.svelte` — prompt state + allowed extensions
```ts
// v1: single prompt text
let promptText = "";

// v1: allowed attachments for prompt mode
let promptAllowedExtensions = ".wav,.png,.mp4";

// prompt attachments live in the existing `files` variable (FileList) to reuse uploadFiles()
// let files: FileList
```

### `DUUIWeb/src/routes/processes/+page.svelte` — prompt UI branch (copy TextArea pattern)
```svelte
{:else if equals($processSettingsStore.input.provider, IO.Prompt)}
    <TextArea
        label="Prompt"
        name="content"
        error={promptText === '' ? 'Text cannot be empty' : ''}
        bind:value={promptText}
    />

    <FileDropzone
        accept={promptAllowedExtensions}
        multiple={true}
        on:files={(e) => onPromptFilesDropped(e.detail.files)}
    />
```

### `DUUIWeb/src/routes/processes/+page.svelte` — enforce max-1-mp4 and update `files: FileList`
```ts
function splitPromptAttachments(fileList: FileList): { wavs: File[]; pngs: File[]; mp4: File | null } {
    const wavs: File[] = [];
    const pngs: File[] = [];
    let mp4: File | null = null;

    for (const f of Array.from(fileList)) {
        const n = f.name.toLowerCase();
        if (n.endsWith(".png")) pngs.push(f);
        else if (n.endsWith(".wav")) wavs.push(f);
        else if (n.endsWith(".mp4") && mp4 == null) mp4 = f;
    }

    return { wavs, pngs, mp4 };
}

function toFileList(files: File[]): FileList {
    const dt = new DataTransfer();
    for (const f of files) dt.items.add(f);
    return dt.files;
}

function onPromptFilesDropped(dropped: FileList) {
    // accept filters picker UI; still enforce max-1-mp4 here
    const merged = [...Array.from(files ?? []), ...Array.from(dropped)];
    const { wavs, pngs, mp4 } = splitPromptAttachments(toFileList(merged));

    const next: File[] = [...pngs, ...wavs];
    if (mp4) next.push(mp4);

    files = toFileList(next);
}
```

### `DUUIWeb/src/routes/processes/+page.svelte` — reuse existing `uploadFiles()` by prepending `promt_1.txt`
```ts
function prependPromptFile() {
    const promptFile = new File([promptText], "promt_1.txt", { type: "text/plain;charset=utf-8" });
    const merged = [promptFile, ...Array.from(files ?? [])];
    files = toFileList(merged);
}

// before calling uploadFiles() in prompt-mode:
// prependPromptFile();
// const ok = await uploadFiles();  // reuse existing function
```

### `DUUIWeb/src/routes/processes/+page.svelte` — upload URL switch (reuse existing params, add `&prompt=true`)
```ts
const fileUpload = await fetch(
	`/api/files/upload?store=${fileStorage.storeFiles}&provider=${fileStorage.provider}&path=${fileStorage.path}&providerId=${fileStorage.provider_id}` +
		`${equals($processSettingsStore.input.provider, IO.Prompt) ? "&prompt=true" : ""}`,
	{ method: "POST", body: formData }
);
```

## Modified

### `DUUIWeb/src/lib/store.ts` — pipeline store state during `uploadFiles()`
```ts
// currentPipelineStore is a localStorage-backed store (not touched by uploadFiles()).
export const currentPipelineStore = localStorageStore("currentPipelineStore", blankPipeline());
```

### `DUUIWeb/src/routes/processes/+page.svelte` — get pipeline name inside `uploadFiles()`
```ts
import { currentPipelineStore } from "$lib/store";

// use $currentPipelineStore anywhere in the component (reactive store value)
const pipelineName = $currentPipelineStore?.name ?? "";

// inside uploadFiles() you can read it too (same value; uploadFiles() does not clear it)
// const pipelineName = $currentPipelineStore?.name ?? "";
```

### `DUUIWeb/src/routes/processes/+page.svelte` — fill `currentPipelineStore` from `pipeline_id` (quick)
```ts
import { currentPipelineStore } from "$lib/store";

async function ensureCurrentPipelineFromId(): Promise<void> {
    const pipelineId = $processSettingsStore.pipeline_id;
    if (!pipelineId) return;

    const res = await fetch(`/api/pipelines?id=${pipelineId}`, { method: "GET" });
    if (!res.ok) return;

    const pipeline = await res.json();
    currentPipelineStore.set(pipeline);
}

// call once after you have set `$processSettingsStore.pipeline_id` from params:
// await ensureCurrentPipelineFromId();
```

### `DUUIWeb/src/routes/processes/+page.svelte` — quick timestamp helper (TS)
```ts
function ts(): string {
    return new Date().toISOString();
}
```

### `DUUIWeb/src/routes/processes/+page.svelte` — quick timestamp helper (local `YYYY-MM-DD_HH-mm-ss`)
```ts
function tsLocalCompact(d: Date = new Date()): string {
    const pad = (n: number) => String(n).padStart(2, "0");
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}_${pad(d.getHours())}-${pad(d.getMinutes())}-${pad(d.getSeconds())}`;
}
```

### `currentPipelineStore` lifecycle on `/processes` (actual flow)
```ts
// 1) Initialization:
// DUUIWeb/src/lib/store.ts
// - currentPipelineStore is loaded from localStorage key "currentPipelineStore"
// - if key missing: blankPipeline() is used

// 2) Set (non-empty) cases:
// DUUIWeb/src/routes/pipelines/[oid]/+page.svelte
// - $currentPipelineStore = pipeline (server-loaded pipeline)
//
// DUUIWeb/src/routes/pipelines/build/+page.svelte
// - edits $currentPipelineStore during pipeline creation/build

// 3) Clear (becomes blankPipeline()) cases:
// DUUIWeb/src/routes/+layout.svelte (logout())
// DUUIWeb/src/lib/svelte/components/Drawer/Sidebar.svelte (logout())
// - currentPipelineStore.set(blankPipeline())

// 4) On DUUIWeb/src/routes/processes/+page.svelte:
// - currentPipelineStore is NOT imported/modified
// - so at the exact moment uploadFiles() runs, $currentPipelineStore is whatever it was before:
//   - last pipeline you visited/edited (persisted), OR
//   - blankPipeline() if you've never set it or you logged out
```

### `DUUIWeb/src/lib/duui/io.ts` — add `Prompt` provider and allow it as input
```ts
export type IOProvider =
    | "Dropbox"
    | "Minio"
    | "File"
    | "Text"
    | "NextCloud"
    | "Google"
    | "LocalDrive"
    | "None"
    | "Prompt";

export enum IO {
    Dropbox = "Dropbox",
    File = "File",
    Minio = "Minio",
    Text = "Text",
    NextCloud = "NextCloud",
    Google = "Google",
    LocalDrive = "LocalDrive",
    None = "None",
    Prompt = "Prompt"
}

export const IO_INPUT: string[] = [
    "Dropbox",
    "File",
    "Minio",
    "Text",
    "NextCloud",
    "Google",
    "LocalDrive",
    "Prompt"
];
```

### `DUUIWeb/src/lib/duui/io.ts` — treat `IO.Prompt` like a stateless provider (validation)
```ts
if (equals(input.provider.toString(), IO.Prompt)) {
    // v1: prompt requires text + at least one file (matching the UI gating)
    return input.content.length > 0 && files?.length > 0;
}
```


# Backend

## New

### `FileUploadUtils.createCas(Collection<Part>)` overload (Java sketch)
```java
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import javax.servlet.http.Part;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;

// in FileUploadUtils
public static JCas createCas(Collection<Part> parts) throws UIMAException, java.io.IOException {
    String language = "en";
    List<String> texts = new ArrayList<>();
    List<String> imagesB64 = new ArrayList<>();
    List<String> audiosB64 = new ArrayList<>();
    String videoB64 = null;

    for (Part part : parts) {
        String filename = part.getSubmittedFileName();
        String lower = filename == null ? "" : filename.toLowerCase();

        // prompt texts are files: promt_<i>.txt
        if (lower.startsWith("promt_") && lower.endsWith(".txt")) {
            try (InputStream is = part.getInputStream()) {
                String txt = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
                if (!txt.isEmpty()) texts.add(txt);
            }
            continue;
        }

        String ct = part.getContentType() == null ? "" : part.getContentType();
        try (InputStream is = part.getInputStream()) {
            String b64 = Base64.getEncoder().encodeToString(is.readAllBytes());
            if (ct.startsWith("image/")) imagesB64.add(b64);
            else if (ct.startsWith("audio/")) audiosB64.add(b64);
            else if (ct.startsWith("video/") && videoB64 == null) videoB64 = b64;
        }
    }

    JCas cas = JCasFactory.createJCas();
    FileUploadUtils.addPrompts(cas, language, texts);
    if (!imagesB64.isEmpty()) FileUploadUtils.addImages(cas, imagesB64);
    if (!audiosB64.isEmpty()) FileUploadUtils.addAudios(cas, audiosB64);
    if (videoB64 != null) FileUploadUtils.addVideo(cas, videoB64);
    return cas;
}
```

### Helper overload sketches to avoid `toArray(...)` (Java)
```java
import java.util.List;
import org.apache.uima.jcas.JCas;
import org.texttechnologylab.annotation.type.Audio;
import org.texttechnologylab.annotation.type.Image;
import org.texttechnologylab.annotation.type.Video;

public static void addImages(JCas cas, List<String> base64Images) {
    for (String image : base64Images) {
        Image img = new Image(cas);
        img.setSrc(image);
        img.addToIndexes();
    }
}

public static void addAudios(JCas cas, List<String> base64Wavs) {
    for (String wav : base64Wavs) {
        Audio audio = new Audio(cas);
        audio.setSrc(wav);
        audio.setMimetype("audio/wav");
        audio.addToIndexes();
    }
}

public static void addVideo(JCas cas, String base64Mp4) {
    Video videoWrapper = new Video(cas);
    videoWrapper.setMimetype("video/mp4");
    videoWrapper.setSrc(base64Mp4);
    videoWrapper.addToIndexes();
}
```

## Modified

### `Main.uploadFile(...)` prompt switch (Java sketch)
```java
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;

import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.jcas.JCas;

boolean isPrompt = request.queryParamOrDefault("prompt", "false").equalsIgnoreCase("true");

request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
Collection<Part> parts = request.raw().getParts();

if (!isPrompt) {
    // existing behavior: save each `file` part as-is
    for (Part part : parts) {
        if (!part.getName().equals("file")) continue;
        // Files.copy(part.getInputStream(), ...)
    }
} else {
    // prompt behavior: create CAS/XMI from raw parts
    JCas cas = FileUploadUtils.createCas(parts);
    Path promptXmi = Paths.get(root.toString(), "prompt.xmi");
    try (OutputStream out = Files.newOutputStream(promptXmi)) {
        XmiCasSerializer.serialize(cas.getCas(), out);
    }

    // then proceed with existing store=true flow (it now includes prompt.xmi)
}
```
