```svelte
<!-- routes/processes/+page.svelte (ONLY the route; panels are separate blocks below) -->
<script lang="ts">
  import { IO, IO_INPUT, IO_OUTPUT, INPUT_EXTENSIONS, OUTPUT_EXTENSIONS, PROMPT_EXTENSIONS } from '$lib/duui/io.js';
  import { processSettingsStore, userSession } from '$lib/store.js';
  import { goto } from '$app/navigation';
  import { onMount } from 'svelte';

  import GeneralIoPanel from '$lib/process-io/GeneralIoPanel.svelte';
  import { Languages } from '$lib/duui/process';

  let inputIo: { initFromParams: (p: URLSearchParams) => void };
  let outputIo: { initFromParams: (p: URLSearchParams) => void };

  const createProcess = async () => {};
  const onCancel = async () => goto(`/pipelines/${$processSettingsStore.pipeline_id}`);

  onMount(() => {
    const params = new URLSearchParams(window.location.search);
    inputIo?.initFromParams(params);
    outputIo?.initFromParams(params);
  });
</script>

<div class="container mx-auto max-w-4xl grid gap-4">
  <div class="grid gap-4">
    <GeneralIoPanel
      kind="input"
      label="Input"
      providerOptions={IO_INPUT}
      extensionOptions={INPUT_EXTENSIONS}
      promptAccept={PROMPT_EXTENSIONS}
      languageOptions={Languages}
      bind:language={$processSettingsStore.settings.language}
      bind:provider={$processSettingsStore.input}
      user={$userSession}
      bind:this={inputIo}
    />

    <!-- ProcessConfigPanel.svelte (sketch; process settings only) -->

    <GeneralIoPanel
      kind="output"
      label="Output"
      providerOptions={IO_OUTPUT}
      extensionOptions={OUTPUT_EXTENSIONS}
      bind:provider={$processSettingsStore.output}
      user={$userSession}
      bind:this={outputIo}
    />
  </div>
</div>
```

```svelte
<!-- $lib/process-io/GeneralIoPanel.svelte (COMPONENT, NOT the route) -->
<script lang="ts">
  import { IO, hasFolderPicker, isStatelessProvider, getCloudProviderAliases, type IOProvider } from '$lib/duui/io.js';
  import { equals } from '$lib/duui/utils/text';
  import { isValidInput, isValidOutput } from '$lib/duui/io.js';

  import Dropdown from '$lib/svelte/components/Input/Dropdown.svelte';
  import { faCheck, faWarning } from '@fortawesome/free-solid-svg-icons';
  import Fa from 'svelte-fa';

  import TextPanel from '$lib/process-io/TextPanel.svelte';
  import UploadPanel from '$lib/process-io/UploadPanel.svelte';
  import LocalDirectoryPanel from '$lib/process-io/LocalDirectoryPanel.svelte';
  import FileUploadPanel from '$lib/process-io/FileUploadPanel.svelte';
  import RemoteDirectoryPanel from '$lib/process-io/RemoteDirectoryPanel.svelte';

  import type { DUUIDocumentProvider } from '$lib/duui/io.js';
  import type { User } from '$lib/types'; /* sketch */

  export let kind: 'input' | 'output';
  export let label: string;
  export let provider: DUUIDocumentProvider;
  export let user: User;

  export let providerOptions: IOProvider[];
  export let extensionOptions: string[];
  export let promptAccept: string | undefined;

  export let languageOptions: string[] | undefined;
  export let language: string | undefined;

  const hasConnections = (providerLower: string) => Object.keys(user?.connections?.[providerLower] ?? {}).length > 0;

  const isError =
    kind === 'input'
      ? ![IO.File, IO.Text, IO.None, IO.LocalDrive].includes(provider.provider as IO) &&
        !hasConnections(provider.provider.toLowerCase())
      : ![IO.File, IO.Text, IO.None, IO.LocalDrive].includes(provider.provider as IO) &&
        !hasConnections(provider.provider.toLowerCase());

  const isValid =
    kind === 'input'
      ? isValidInput(provider, /* internal files */ files, user)
      : isValidOutput(provider, user);

  // internal upload-only state
  let files: FileList;

  let ioError: string | null = null;
</script>

<div class="section-wrapper space-y-4 p-4 {isValid ? '!border-success-500' : '!border-error-500'}">
  <div class="flex-center-4 justify-between">
    <h2 class="h2">{label}</h2>
    {#if isValid}<Fa icon={faCheck} class="text-success-500" size="2x" />{/if}
  </div>

  {#if isError}
    <div class="text-center w-full variant-soft-error p-4 rounded-md">
      <p class="mx-auto">
        To use {provider.provider} you must first connect your Account
        <a class="anchor" href="/account?tab=1" target="_blank">Account</a>
      </p>
    </div>
  {/if}

  {#if ioError}
    <div class="text-center w-full variant-soft-error p-4 rounded-md">
      <p class="mx-auto">{ioError}</p>
    </div>
  {/if}

  <div class="grid gap-4">
    <div class="flex-center-4">
      <div class="flex-1">
        <Dropdown
          label={kind === 'input' ? 'Source' : 'Target'}
          options={providerOptions}
          bind:value={provider.provider}
          on:change={() => {
            provider.provider_id = '';
            provider.path = '';
            provider.content = '';
          }}
        />
      </div>

      {#if !isError && !equals(provider.provider, IO.Text) && !isStatelessProvider(provider.provider)}
        {#if hasConnections(provider.provider.toLowerCase())}
          <Dropdown
            label="Connection Alias"
            name={`${kind}-alias`}
            options={getCloudProviderAliases(user?.connections?.[provider.provider.toLowerCase()] ?? {})}
            initFirst={true}
            bind:value={provider.provider_id}
          />
        {/if}
      {/if}

      {#if !equals(provider.provider, IO.Text) && (kind === 'input' ? true : !isStatelessProvider(provider.provider))}
        <Dropdown
          label="File extension"
          name={`${kind}-extension`}
          options={extensionOptions}
          bind:value={provider.file_extension}
        />
      {/if}
    </div>

    {#if kind === 'input' && languageOptions && language !== undefined}
      <Dropdown label="Language" options={languageOptions} bind:value={language} />
    {/if}
  </div>

  {#if kind === 'input' && equals(provider.provider, IO.Text)}
    <TextPanel bind:provider />
  {:else if kind === 'input' && (equals(provider.provider, IO.File) || equals(provider.provider, IO.Prompt))}
    <UploadPanel bind:provider {promptAccept} fileAccept={provider.file_extension} bind:files />
    <FileUploadPanel {user} />
  {:else if equals(provider.provider, IO.LocalDrive)}
    <LocalDirectoryPanel
      name={kind === 'input' ? 'inputPaths' : 'outputPaths'}
      isMultiple={kind === 'input'}
      bind:value={provider.path}
    />
  {:else if hasFolderPicker(provider.provider, true) && provider.provider_id}
    <RemoteDirectoryPanel
      provider={provider.provider}
      providerId={provider.provider_id}
      userOid={user.oid}
      connections={user.connections}
      name={kind === 'input' ? 'inputPaths' : 'outputPaths'}
      isMultiple={kind === 'input'}
      bind:selectedPaths={provider.path}
      on:error={(e) => (ioError = e.detail.message)}
      on:clearError={() => (ioError = null)}
    />
  {:else if equals(provider.provider, IO.Minio)}
    <!-- Minio path TextInput lives here in real page -->
  {/if}
</div>
```

```svelte
<!-- $lib/process-io/TextPanel.svelte (COMPONENT) -->
<script lang="ts">
  import { IO } from '$lib/duui/io.js';
  import TextArea from '$lib/svelte/components/Input/TextArea.svelte';

  import type { DUUIDocumentProvider } from '$lib/duui/io.js';
  export let provider: DUUIDocumentProvider;
</script>

<div class="space-y-2">
  <TextArea
    label="Document Text"
    name="content"
    error={provider.content === '' ? 'Text cannot be empty' : ''}
    bind:value={provider.content}
  />
</div>
```

```svelte
<!-- $lib/process-io/UploadPanel.svelte (COMPONENT) -->
<script lang="ts">
  import { IO } from '$lib/duui/io.js';
  import { FileDropzone } from '@skeletonlabs/skeleton';
  import type { AcceptTypes } from '@skeletonlabs/skeleton';
  import type { DUUIDocumentProvider } from '$lib/duui/io.js';
  import TextPanel from '$lib/process-io/TextPanel.svelte';

  export let provider: DUUIDocumentProvider;
  export let promptAccept: AcceptTypes;
  export let fileAccept: AcceptTypes;
  export let files: FileList;

  let promptText = '';

  const onPromptFilesDropped = (dropped: FileList) => {};
</script>

{#if provider.provider === IO.Prompt}
  <div class="space-y-4">
    <TextPanel provider={{ ...provider, content: promptText }} />
    <div class="space-y-1">
      <p class="form-label">File</p>
      <FileDropzone
        name="inputFile"
        on:files={(e) => onPromptFilesDropped(e.detail.files)}
        accept={promptAccept}
        multiple={true}
        border="border border-color"
        rounded="rounded-md"
        class="input-wrapper"
      />
      <p class="form-label {(files?.length || 0) === 0 ? 'text-error-500' : ''}">
        {files?.length || 0} files selected
      </p>
    </div>
  </div>
{:else if provider.provider === IO.File}
  <div class="space-y-1">
    <p class="form-label">File</p>
    <FileDropzone
      name="inputFile"
      bind:files
      accept={fileAccept}
      multiple={true}
      border="border border-color"
      rounded="rounded-md"
      class="input-wrapper"
    />
    <p class="form-label {(files?.length || 0) === 0 ? 'text-error-500' : ''}">
      {files?.length || 0} files selected
    </p>
  </div>
{/if}
```

```svelte
<!-- $lib/process-io/RemoteDirectoryPanel.svelte (COMPONENT; tree picker OR path text input only) -->
<script lang="ts">
  import { IO, hasFolderPicker } from '$lib/duui/io.js';
  import _ from 'lodash';
  import { equals } from '$lib/duui/utils/text';
  import FolderStructure from '$lib/svelte/components/Input/FolderStructure.svelte';
  import TextInput from '$lib/svelte/components/Input/TextInput.svelte';
  import { faRepeat } from '@fortawesome/free-solid-svg-icons';
  import Fa from 'svelte-fa';
  import type { TreeViewNode } from '@skeletonlabs/skeleton';
  import { onMount } from 'svelte';
  import type { IOProvider } from '$lib/duui/io.js';
  import { createEventDispatcher } from 'svelte';

  const { isEmpty } = _;

  export let provider: IOProvider;
  export let providerId: string;
  export let selectedPaths: string;

  export let isMultiple: boolean;
  export let name: string;

  export let userOid: string;
  export let connections: Record<string, Record<string, unknown>>;

  let remoteTree: TreeViewNode | undefined;
  let fetchingTree = false;
  const dispatch = createEventDispatcher<{ error: { message: string }; clearError: void }>();

  const fetchRemoteTree = async (reset: boolean) => {
    if (!hasFolderPicker(provider, true)) return;
    if (!providerId) return;
    if (!connections?.[provider.toLowerCase()]?.[providerId]) return;

    fetchingTree = true;
    const response = await fetch('/api/processes/folderstructure', {
      method: 'POST',
      body: JSON.stringify({ provider, user: userOid, reset, providerId })
    });
    fetchingTree = false;

    if (response.ok) {
      remoteTree = (await response.json()) as TreeViewNode;
      dispatch('clearError');
    } else {
      dispatch('error', { message: `Failed to fetch folder structure: ${response.statusText}` });
    }
  };

  onMount(async () => {
    if (providerId) await fetchRemoteTree(false);
  });
</script>

{#if !isEmpty(providerId) && hasFolderPicker(provider, true)}
  <div class="flex w-full">
    {#if !remoteTree}
      <div class="w">
        <TextInput
          label="Relative path"
          name="relativePath"
          bind:value={selectedPaths}
          error={selectedPaths === '/' ? 'Provide an empty path to select the root folder.' : ''}
        />
      </div>
    {:else}
      <div class="w-11/12">
        <FolderStructure
          tree={remoteTree}
          label="Folder Picker"
          {name}
          {isMultiple}
          bind:value={selectedPaths}
        />
      </div>
      <div class="w-1/12">
        <span class="form-label text-start">Refresh</span>
        <button
          class="p-3 mt-1 ml-3 rounded-md hover:bg-primary-500 hover:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-opacity-50"
          on:click={() => fetchRemoteTree(true)}
        >
          <Fa icon={faRepeat} />
        </button>
      </div>
    {/if}
  </div>
{/if}
```

```svelte
<!-- (intentionally omitted; uses existing `FolderStructure.svelte`) -->
```

```svelte
<!-- $lib/process-io/LocalDirectoryPanel.svelte (COMPONENT) -->
<script lang="ts">
  import FolderStructure from '$lib/svelte/components/Input/FolderStructure.svelte';
  import Tip from '$lib/svelte/components/Tip.svelte';
  import { faWarning } from '@fortawesome/free-solid-svg-icons';
  import type { TreeViewNode } from '@skeletonlabs/skeleton';
  import { onMount } from 'svelte';

  export let name: string;
  export let isMultiple: boolean;
  export let value: string;

  let lfs: TreeViewNode | undefined;

  onMount(async () => {
    const response = await fetch('/api/settings/filtered-folder-structure', { method: 'GET' });
    if (response.ok) lfs = (await response.json()) as TreeViewNode;
  });
</script>

{#if Object.keys(lfs || {}).length > 0}
  <FolderStructure tree={lfs} label="Folder Picker" {name} {isMultiple} bind:value />
{:else}
  <Tip tipTheme="error" customIcon={faWarning}>
    The local drive is not available. Please select a different provider.
    To gain access to the local drive, please contact your administrator.
  </Tip>
{/if}
```
