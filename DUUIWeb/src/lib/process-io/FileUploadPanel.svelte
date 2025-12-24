<script lang="ts">
	import { IO, getCloudProviderAliases, hasFolderPicker, isValidFileUpload, type IOProvider } from '$lib/duui/io.js'
	import { equals } from '$lib/duui/utils/text'
	import { isEmpty } from 'lodash'

	import Checkbox from '$lib/svelte/components/Input/Checkbox.svelte'
	import Dropdown from '$lib/svelte/components/Input/Dropdown.svelte'
	import TextInput from '$lib/svelte/components/Input/TextInput.svelte'

	import LocalDirectoryPanel from '$lib/process-io/LocalDirectoryPanel.svelte'
	import RemoteDirectoryPanel from '$lib/process-io/RemoteDirectoryPanel.svelte'

	export let userOid: string
	export let connections: Record<string, Record<string, unknown>>

	let fileStorage: {
		storeFiles: boolean
		provider: IOProvider
		provider_id: string
		path: string
	} = { storeFiles: false, provider: IO.LocalDrive, provider_id: '', path: '' }

	const hasConnections = (providerLower: string) => Object.keys(connections?.[providerLower] ?? {}).length > 0

	$: isValidFileStorage = !fileStorage.storeFiles || isValidFileUpload(fileStorage as any)
	$: isFileUploadError =
		fileStorage.storeFiles &&
		(!equals(fileStorage.provider, IO.LocalDrive) && !hasConnections(fileStorage.provider.toLowerCase())) &&
		(equals(fileStorage.provider, IO.LocalDrive) && false)

	$: isFileUploadRequirementsMet =
		(!equals(fileStorage.provider, IO.LocalDrive) && !isEmpty(fileStorage.provider_id)) ||
		(equals(fileStorage.provider, IO.LocalDrive) && true)

	export const getFileStorage = () => fileStorage
	export const getIsValidFileStorage = () => isValidFileStorage
</script>

<Checkbox label="Upload input files to cloud storage." bind:checked={fileStorage.storeFiles} />

{#if fileStorage.storeFiles}
	<div
		class="grid gap-4"
		class:grid-cols-1={!hasConnections(fileStorage.provider.toLowerCase())}
		class:grid-cols-2={hasConnections(fileStorage.provider.toLowerCase())}
	>
		<Dropdown
			label="Provider"
			options={[IO.LocalDrive, IO.Dropbox, IO.Minio, IO.NextCloud, IO.Google]}
			bind:value={fileStorage.provider}
			on:change={() => {
				fileStorage.provider_id = ''
			}}
		/>
		{#if !isFileUploadError && hasConnections(fileStorage.provider.toLowerCase())}
			<Dropdown
				label="Connection Alias"
				options={getCloudProviderAliases(connections?.[fileStorage.provider.toLowerCase()] ?? {})}
				initFirst={true}
				bind:value={fileStorage.provider_id}
			/>
		{/if}
	</div>

	{#if !isFileUploadError && isFileUploadRequirementsMet}
		{#if equals(fileStorage.provider, IO.Minio)}
			<TextInput
				label="Path (bucket/path/to/folder)"
				name="fileStoragePath"
				bind:value={fileStorage.path}
			/>
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
{/if}
