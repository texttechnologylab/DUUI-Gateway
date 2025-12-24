<script lang="ts">
	/*
	NOTE: This component is a 1:1 refactor extraction of the existing markup/logic from
	`DUUIWeb/src/routes/processes/+page.svelte`. Keep structure/classes identical.
	*/

	import { IO, hasFolderPicker, isStatelessProvider, getCloudProviderAliases } from '$lib/duui/io.js'
	import {
		isValidFileUpload,
		isValidInput,
		isValidOutput,
		isValidS3BucketName,
		splitPromptAttachments,
		toFileList,
		type FileExtension,
		type IOProvider,
		type OutputFileExtension,
		PROMPT_EXTENSIONS
	} from '$lib/duui/io.js'
	import { equals } from '$lib/duui/utils/text'
	import { errorToast } from '$lib/duui/utils/ui'
	import type { DUUIDocumentProvider } from '$lib/duui/io.js'
	import type { TreeViewNode } from '@skeletonlabs/skeleton'
	import { FileDropzone, getToastStore } from '@skeletonlabs/skeleton'
	import Fa from 'svelte-fa'
	import { faCheck, faRepeat, faWarning } from '@fortawesome/free-solid-svg-icons'
	import { onMount } from 'svelte'
	import _ from 'lodash'
	const { isEmpty } = _
	import {
		ProcessPagePhase,
		type ProcessPageState,
		type ProcessPageStateStore
	} from '$lib/process-io/pageState'

	import Checkbox from '$lib/svelte/components/Input/Checkbox.svelte'
	import Dropdown from '$lib/svelte/components/Input/Dropdown.svelte'
	import FolderStructure from '$lib/svelte/components/Input/FolderStructure.svelte'
	import TextArea from '$lib/svelte/components/Input/TextArea.svelte'
	import TextInput from '$lib/svelte/components/Input/TextInput.svelte'
	import Tip from '$lib/svelte/components/Tip.svelte'

	export let kind: 'input' | 'output'
	export let provider: DUUIDocumentProvider
	export let user: any
	export let pageState: ProcessPageStateStore | null = null

	export let providerOptions: string[]
	export let extensionOptions: string[]

	export let label: string
	export let aliasName: string
	export let extensionName: string
	export let folderName: string
	export let folderIsMultiple: boolean

	export let languageOptions: string[] | undefined = undefined
	export let language: string | undefined = undefined

	export let valid = false
	export let isProviderError = false
	export let validFileStorage = true
	export let errorPath: string = '/account?tab=1'

	const toastStore = getToastStore()

	let lfs: TreeViewNode
	let tree: TreeViewNode | undefined

	let files: FileList
	let promptText = ''
	let uploading = false
	let filesCount = 0
	let fetchingTree = false

	let fileStorage = {
		storeFiles: false,
		provider: IO.LocalDrive as IOProvider,
		provider_id: '',
		path: ''
	}
	let fileUploadTree: TreeViewNode | undefined
	let fileUploadAliases: Map<string, string> = new Map()

	const hasConnections = (providerLower: string) =>
		Object.keys(user?.connections?.[providerLower] ?? {}).length > 0

	onMount(() => {
		getLFS().catch(() => {
			/* ignore */
		})
	})

	export const initFromParams = (params: URLSearchParams) => {
		if (kind === 'input') {
			provider.provider = (params.get('input_provider') as IOProvider) || provider.provider
			provider.provider_id ||= params.get('input_provider_id') || ''
			provider.path = params.get('input_path') || provider.path
			provider.content = params.get('input_content') || provider.content
			provider.file_extension =
				(params.get('input_file_extension') as FileExtension) || provider.file_extension
			if (language !== undefined) language = params.get('language') || language
		} else {
			provider.provider = (params.get('output_provider') as IOProvider) || provider.provider
			provider.provider_id ||= params.get('output_provider_id') || ''
			provider.path = params.get('output_path') || provider.path
			provider.content = params.get('output_content') || provider.content
			provider.file_extension =
				(params.get('output_file_extension') as OutputFileExtension) || provider.file_extension
		}
	}

	async function getLFS() {
		try {
			const response = await fetch('/api/settings/filtered-folder-structure', { method: 'GET' })
			if (response.ok) lfs = (await response.json()) as TreeViewNode
		} catch {
			toastStore.trigger(errorToast('Failed to fetch local file system structure: '))
		}
	}

	const getFolderStructure = async (providerX: IOProvider, providerId: string, isReset: boolean) => {
		if (
			!hasFolderPicker(providerX, true) ||
			!providerId ||
			!user?.connections?.[providerX.toLowerCase()]?.[providerId]
		) {
			return undefined
		}

		fetchingTree = true
		pageState?.update((s: ProcessPageState) => ({ ...s, phase: ProcessPagePhase.DirectoryFetching }))
		const response = await fetch('/api/processes/folderstructure', {
			method: 'POST',
			body: JSON.stringify({
				provider: providerX,
				user: user?.oid,
				reset: isReset,
				providerId
			})
		})
		fetchingTree = false
		pageState?.update((s: ProcessPageState) => ({ ...s, phase: ProcessPagePhase.Editing }))

		if (response.ok) return (await response.json()) as TreeViewNode
		toastStore.trigger(errorToast('Failed to fetch folder structure: ' + response.statusText))
		return undefined
	}

	const setTree = async (providerX: IOProvider, providerId: string, isReset: boolean) => {
		tree = await getFolderStructure(providerX, providerId, isReset)
	}

	const setFileUploadTree = async (providerX: IOProvider, providerId: string, isReset: boolean) => {
		fileUploadTree = await getFolderStructure(providerX, providerId, isReset)
	}

	function onPromptFilesDropped(dropped: FileList) {
		const merged = [...Array.from(files ?? []), ...Array.from(dropped)]
		const { wavs, pngs, mp4 } = splitPromptAttachments(toFileList(merged))

		const next: File[] = [...pngs, ...wavs]
		if (mp4) next.push(mp4)

		files = toFileList(next)
	}

	export const uploadIfNeeded = async (pipelineId: string) => {
		if (!(kind === 'input' && (equals(provider.provider, IO.File) || equals(provider.provider, IO.Prompt))))
			return true
		if (!files) return false

		uploading = true
		filesCount = files?.length || 0
		pageState?.update((s: ProcessPageState) => ({
			...s,
			phase: ProcessPagePhase.Uploading,
			uploadFilesCount: filesCount
		}))

		let filesToUpload: FileList = files
		if (equals(provider.provider, IO.Prompt)) {
			const promptFile = new File([promptText], 'promt_1.txt', { type: 'text/plain;charset=utf-8' })
			const merged = [promptFile, ...Array.from(files ?? [])]
			filesToUpload = toFileList(merged)
		}

		const formData = new FormData()
		for (const file of filesToUpload) formData.append('file', file, file.name)

		const params = new URLSearchParams()
		params.append('store', String(fileStorage.storeFiles))
		params.append('provider', fileStorage.provider)
		params.append('path', fileStorage.path)
		params.append('providerId', fileStorage.provider_id)
		params.append('prompt', String(equals(provider.provider, IO.Prompt)))
		params.append('pipelineId', pipelineId)

		const fileUpload = await fetch(`/api/files/upload?${params}`, { method: 'POST', body: formData })

		if (!fileUpload.ok) {
			const errorMessage = await fileUpload.text()
			toastStore.trigger(errorToast('ERROR: ' + errorMessage + ' '))
			files = []
			uploading = false
			pageState?.update((s: ProcessPageState) => ({
				...s,
				phase: ProcessPagePhase.Editing,
				uploadFilesCount: 0
			}))
			return false
		}

		const json = await fileUpload.json()
		provider.path = json.path
		uploading = false
		pageState?.update((s: ProcessPageState) => ({
			...s,
			phase: ProcessPagePhase.Editing,
			uploadFilesCount: 0
		}))
		return true
	}

	$: filesCount = files?.length || 0

	$: isProviderError =
		![IO.File, IO.Text, IO.None, IO.LocalDrive].includes(provider.provider as IO) &&
		!hasConnections(provider.provider.toLowerCase())

	$: valid =
		kind === 'input'
			? isValidInput(provider, files, user)
			: isValidOutput(provider, user)

	$: fileUploadAliases = hasConnections(fileStorage.provider.toLowerCase())
		? getCloudProviderAliases(user?.connections?.[fileStorage.provider.toLowerCase()])
		: new Map()

	$: validFileStorage =
		kind !== 'input' ||
		provider.provider !== IO.File ||
		!fileStorage.storeFiles ||
		isValidFileUpload(fileStorage as any)

	$: if (!tree && !isProviderError && provider.provider_id && hasConnections(provider.provider.toLowerCase())) {
		setTree(provider.provider, provider.provider_id, false)
	}

	$: if (!fileUploadTree && fileStorage.provider_id && hasConnections(fileStorage.provider.toLowerCase())) {
		setFileUploadTree(fileStorage.provider, fileStorage.provider_id, false)
	}

	$: if (provider.provider === IO.File) {
		provider.path = ''
	}
	$: if (provider.provider !== IO.Text) {
		provider.content = ''
	}
</script>

<div
	class="section-wrapper space-y-4 p-4 {valid ? '!border-success-500 ' : '!border-error-500'} {kind ===
	'output'
		? 'flex flex-col justify-start relative'
		: ''}"
>
	{#if kind === 'output' && isProviderError}
		<div class="text-center w-full variant-soft-error p-4 rounded-md">
			<p class="mx-auto">
				To use {provider.provider} you must first connect your
				<a class="anchor" href={errorPath} target="_blank">Account</a> and set an alias for your
				connection.
			</p>
		</div>
	{/if}

	<div class="flex-center-4 justify-between">
		<h2 class="h2">{label}</h2>
		{#if valid}
			<Fa icon={faCheck} class="text-success-500" size="2x" />
		{/if}
	</div>

		{#if kind === 'input' && (isProviderError || (provider.provider === IO.File && fileStorage.storeFiles && !validFileStorage))}
			<div class="text-center w-full variant-soft-error p-4 rounded-md">
				<p class="mx-auto">
					{isProviderError
						? 'To use ' + provider.provider + ' you must first connect your Account'
						: 'To use ' + fileStorage.provider + ' you must first connect your Account'}
					<a class="anchor" href={errorPath} target="_blank">Account</a>
				</p>
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
							provider.provider_id = ''
							provider.path = ''
							files = []
							promptText = ''
							tree = undefined
						}}
					/>
				</div>
				{#if !isProviderError && !equals(provider.provider, IO.Text)}
					{#if !isStatelessProvider(provider.provider) && hasConnections(provider.provider.toLowerCase())}
						{#key provider.provider}
							<Dropdown
								label="Connection Alias"
								name={aliasName}
								options={getCloudProviderAliases(user?.connections?.[provider.provider.toLowerCase()])}
								initFirst={true}
								bind:value={provider.provider_id}
								on:change={() => setTree(provider.provider, provider.provider_id, false)}
							/>
						{/key}
					{/if}
					{#if kind === 'input' || (kind === 'output' && !isStatelessProvider(provider.provider))}
						<Dropdown
							label="File extension"
							name={extensionName}
							options={extensionOptions}
							bind:value={provider.file_extension}
						/>
					{/if}
				{/if}
			</div>

			{#if kind === 'input' && languageOptions && language !== undefined}
				<Dropdown label="Language" options={languageOptions} bind:value={language} />
			{/if}

			{#if kind === 'input' && equals(provider.provider, IO.Text)}
				<TextArea
					label="Document Text"
					name="content"
					error={provider.content === '' ? 'Text cannot be empty' : ''}
					bind:value={provider.content}
				/>
			{:else if kind === 'input' && (equals(provider.provider, IO.Prompt) || equals(provider.provider, IO.File))}
				{#if equals(provider.provider, IO.Prompt)}
					<TextArea
						label="Prompt"
						name="content"
						error={promptText === '' ? 'Text cannot be empty' : ''}
						bind:value={promptText}
					/>
					<div class="space-y-1 ">
						<p class="form-label">File</p>
						<FileDropzone
							name="inputFile"
							on:files={(e) => onPromptFilesDropped(e.detail.files)}
							accept={PROMPT_EXTENSIONS}
							multiple={true}
							border="border border-color"
							rounded="rounded-md"
							class="input-wrapper"
						/>
						<p class="form-label {(files?.length || 0) === 0 ? 'text-error-500' : ''}">
							{files?.length || 0} files selected
						</p>
					</div>
				{:else if equals(provider.provider, IO.File)}
					<div class="space-y-1 ">
						<p class="form-label">File</p>
						<FileDropzone
							name="inputFile"
							bind:files
							accept={provider.file_extension}
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

				<Checkbox
					label="Upload input files to cloud storage."
					bind:checked={fileStorage.storeFiles}
				/>

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
								fileUploadTree = undefined
							}}
						/>
						{#if hasConnections(fileStorage.provider.toLowerCase())}
							<Dropdown
								label="Connection Alias"
								options={fileUploadAliases}
								initFirst={true}
								bind:value={fileStorage.provider_id}
								on:change={() =>
									setFileUploadTree(fileStorage.provider, fileStorage.provider_id, false)}
							/>
						{/if}
					</div>

					{#if fileStorage.provider === IO.Minio}
						<TextInput
							label="Path (bucket/path/to/folder)"
							name="fileStoragePath"
							bind:value={fileStorage.path}
						/>
					{:else if fileStorage.provider === IO.LocalDrive}
						<FolderStructure
							tree={lfs}
							label="Folder Picker"
							name="fileLFSUploadPaths"
							isMultiple={false}
							bind:value={fileStorage.path}
						/>
					{:else if fileUploadTree}
						<div class="flex w-full">
							<div class="w-11/12">
								<FolderStructure
									tree={fileUploadTree}
									label="Folder Picker"
									name="fileUploadPaths"
									isMultiple={false}
									bind:value={fileStorage.path}
								/>
							</div>
							<div class="w-1/12">
								<span class="form-label text-start">Refresh</span>
								<button
									class="p-3 mt-1 ml-3 rounded-md hover:bg-primary-500 hover:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-opacity-50"
									on:click={() => setFileUploadTree(fileStorage.provider, fileStorage.provider_id, true)}
								>
									<Fa icon={faRepeat} />
								</button>
							</div>
						</div>
					{:else}
						<TextInput label="Path" name="fileStoragePath" bind:value={fileStorage.path} />
					{/if}
				{/if}
			{:else if equals(provider.provider, IO.LocalDrive)}
				{#if lfs && Object.keys(lfs).length > 0}
					<FolderStructure
						tree={lfs}
						label="Folder Picker"
						name={folderName}
						isMultiple={folderIsMultiple}
						bind:value={provider.path}
					/>
				{:else}
					<Tip tipTheme="error" customIcon={faWarning}>
						The local drive is not available. Please select a different provider. To gain access to
						the local drive, please contact your administrator.
					</Tip>
				{/if}
			{:else if !isProviderError && !isEmpty(provider.provider_id) && equals(provider.provider, IO.Minio)}
				<TextInput
					label="Path (bucket/path/to/folder)"
					error={isValidS3BucketName(provider.path)}
					name={folderName}
					bind:value={provider.path}
				/>
			{:else if !isProviderError && !isEmpty(provider.provider_id)}
				<div class="flex w-full">
					{#if !tree}
						<div class="w">
							<TextInput
								label="Relative path"
								name={folderName}
								bind:value={provider.path}
								error={provider.path === '/' ? 'Provide an empty path to select the root folder.' : ''}
							/>
							<Tip>Do not include Apps/Docker Unified UIMA Interface in your path!</Tip>
						</div>
					{:else}
						<div class="w-11/12">
							<FolderStructure
								tree={tree}
								label="Folder Picker"
								name={folderName}
								isMultiple={folderIsMultiple}
								bind:value={provider.path}
							/>
						</div>
						<div class="w-1/12">
							<span class="form-label text-start">Refresh</span>
							<button
								class="p-3 mt-1 ml-3 rounded-md hover:bg-primary-500 hover:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-opacity-50"
								on:click={() => setTree(provider.provider, provider.provider_id, true)}
							>
								<Fa icon={faRepeat} />
							</button>
						</div>
					{/if}
				</div>
			{/if}
		</div>

		<slot name="settings" />
	</div>
