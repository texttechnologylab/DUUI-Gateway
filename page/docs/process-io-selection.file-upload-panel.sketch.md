```svelte
<!-- FileUploadPanel.svelte -->
<script lang="ts">
  import { IO, hasFolderPicker, type IOProvider } from '$lib/duui/io.js';
  import { equals } from '$lib/duui/utils/text';
  import Checkbox from '$lib/svelte/components/Input/Checkbox.svelte';
  import Dropdown from '$lib/svelte/components/Input/Dropdown.svelte';
  import TextInput from '$lib/svelte/components/Input/TextInput.svelte';

  import RemoteDirectoryPanel from '$lib/process-io/RemoteDirectoryPanel.svelte';
  import LocalDirectoryPanel from '$lib/process-io/LocalDirectoryPanel.svelte';
  import { getCloudProviderAliases, isStatelessProvider } from '$lib/duui/io.js';

  export let userOid: string;
  export let connections: Record<string, Record<string, unknown>>;

  let fileStorage: {
    storeFiles: boolean;
    provider: IOProvider;
    provider_id: string;
    path: string;
  } = { storeFiles: false, provider: IO.LocalDrive, provider_id: '', path: '' };
</script>

<Checkbox label="Upload input files to cloud storage." bind:checked={fileStorage.storeFiles} />

{#if fileStorage.storeFiles}
  <div class="grid gap-4">
    <Dropdown
      label="Provider"
      options={[IO.LocalDrive, IO.Dropbox, IO.Minio, IO.NextCloud, IO.Google]}
      bind:value={fileStorage.provider}
      on:change={() => {
        fileStorage.provider_id = '';
        fileStorage.path = '';
      }}
    />

    {#if !isStatelessProvider(fileStorage.provider) && Object.keys(connections?.[fileStorage.provider.toLowerCase()] ?? {}).length > 0}
      <Dropdown
        label="Connection Alias"
        name="file-upload-alias"
        options={getCloudProviderAliases(connections?.[fileStorage.provider.toLowerCase()] ?? {})}
        initFirst={true}
        bind:value={fileStorage.provider_id}
      />
    {/if}
  </div>

  {#if equals(fileStorage.provider, IO.Minio)}
    <TextInput label="Path (bucket/path/to/folder)" name="fileStoragePath" bind:value={fileStorage.path} />
  {:else if equals(fileStorage.provider, IO.LocalDrive)}
    <LocalDirectoryPanel name="fileLFSUploadPaths" isMultiple={false} bind:value={fileStorage.path} />
  {:else if hasFolderPicker(fileStorage.provider, true)}
    <RemoteDirectoryPanel
      provider={fileStorage.provider}
      providerId={fileStorage.provider_id}
      {userOid}
      {connections}
      name="fileUploadPaths"
      isMultiple={false}
      bind:selectedPaths={fileStorage.path}
    />
  {:else}
    <TextInput label="Path" name="fileStoragePath" bind:value={fileStorage.path} />
  {/if}
{/if}
```
