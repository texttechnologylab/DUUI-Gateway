<script lang="ts">
// <script >
	import { goto } from '$app/navigation'
	import { page } from '$app/stores'
	import {
		INPUT_EXTENSIONS,
		IO,
		IO_INPUT,
		IO_OUTPUT,
		OUTPUT_EXTENSIONS,
		areSettingsValid,
		isValidFileUpload,
		isValidIO,
		isValidInput,
		isValidOutput,
		isValidS3BucketName,
		getCloudProviderAliases,
		type FileExtension,
		type OutputFileExtension,
		type IOProvider,

		isStatelessProvider,

		hasFolderPicker,

		isValidCloudProvider

	} from '$lib/duui/io.js'
	import { Languages, blankSettings } from '$lib/duui/process'
	import { equals } from '$lib/duui/utils/text'
	import { errorToast } from '$lib/duui/utils/ui'
	import { processSettingsStore, userSession } from '$lib/store.js'
	import Checkbox from '$lib/svelte/components/Input/Checkbox.svelte'
	import Dropdown from '$lib/svelte/components/Input/Dropdown.svelte'
	import FolderStructure from "$lib/svelte/components/Input/FolderStructure.svelte";
	import Number from '$lib/svelte/components/Input/Number.svelte'
	import TextArea from '$lib/svelte/components/Input/TextArea.svelte'
	import TextInput from '$lib/svelte/components/Input/TextInput.svelte'
	import Tip from '$lib/svelte/components/Tip.svelte'
import {
	faArrowLeft,
	faCheck,
	faCloud,
	faCloudUpload,
	faFileArrowUp, faRefresh, faRepeat, faWarning,
} from '@fortawesome/free-solid-svg-icons'
	import Fa from 'svelte-fa'
	import { FileDropzone, ProgressBar, getToastStore, type TreeViewNode } from '@skeletonlabs/skeleton'
	import { onMount } from 'svelte'
	import _ from 'lodash';
	const { set, isEmpty } = _;


	export let data
	const { user } = data

	$userSession = user

	const toastStore = getToastStore()

	let files: FileList
	
	let fileStorage = {
			storeFiles: false,
			provider: IO.LocalDrive,
			provider_id: '',
			path: ''
		}

	let lfs: TreeViewNode
	let inputAliases:  Map<string, string>
	let outputAliases:  Map<string, string>
	let fileUploadAliases:  Map<string, string>

	// Helper: does this provider have any connections?
	function hasConnections(provider: string) {
		const key = provider.toLowerCase();
		return Object.keys($userSession?.connections[key] ?? {}).length > 0;
	}
	
	onMount(async () => {


		$userSession = user

		const getLFS = async () => {
			try {
				const response = await fetch('/api/settings/filtered-folder-structure', {
					method: 'GET'
				})

				if (response.ok) {
					lfs =  (await response.json()) as TreeViewNode
				}
		
			} catch (err) {
				toastStore.trigger(errorToast('Failed to fetch local file system structure: '))
			}
		}

		getLFS().catch(() => {/* ignore error */})

		fileStorage = {
			storeFiles: false,
			provider: IO.LocalDrive,
			provider_id: '',
			path: ''
		}

		inputAliases = hasConnections($processSettingsStore.input.provider) ? getCloudProviderAliases($userSession?.connections[$processSettingsStore.input.provider.toLowerCase()]) : new Map()
		outputAliases = hasConnections($processSettingsStore.output.provider) ? getCloudProviderAliases($userSession?.connections[$processSettingsStore.output.provider.toLowerCase()]) : new Map()
		fileUploadAliases = hasConnections(fileStorage.provider) ? getCloudProviderAliases($userSession?.connections[fileStorage.provider.toLowerCase()]) : new Map()

		fileStorage.provider_id = fileUploadAliases.size > 0 ? Array.from(fileUploadAliases.keys())[0] : ""

		const params = $page.url.searchParams

		const reset = (params.get('reset') || 'false') === 'true'

		if (reset) {
			$processSettingsStore = blankSettings()
			goto(`/processes?pipeline_id=${params.get('pipeline_id')}`)
		}


		$processSettingsStore.pipeline_id =
				params.get('pipeline_id') || $processSettingsStore.pipeline_id

		$processSettingsStore.settings.language =
				params.get('language') || $processSettingsStore.settings.language

		$processSettingsStore.settings.notify =
				params.get('notify') === 'true' || $processSettingsStore.settings.notify

		$processSettingsStore.settings.check_target =
				params.get('check_target') === 'true' || $processSettingsStore.settings.check_target

		$processSettingsStore.settings.recursive =
				params.get('recursive') === 'true' || $processSettingsStore.settings.recursive

		$processSettingsStore.settings.overwrite =
				params.get('overwrite') === 'true' || $processSettingsStore.settings.overwrite

		$processSettingsStore.settings.sort_by_size =
				params.get('sort_by_size') === 'true' || $processSettingsStore.settings.sort_by_size

		$processSettingsStore.settings.ignore_errors =
				params.get('ignore_errors') === 'true' || $processSettingsStore.settings.ignore_errors

		$processSettingsStore.settings.minimum_size = +(
				params.get('minimum_size') || $processSettingsStore.settings.minimum_size
		)
		$processSettingsStore.settings.worker_count = +(
				params.get('worker_count') || $processSettingsStore.settings.worker_count
		)

		$processSettingsStore.input.provider =
				(params.get('input_provider') as IOProvider) || $processSettingsStore.input.provider

		$processSettingsStore.input.provider_id ||=
				params.get('input_provider_id') || Array.from(inputAliases.keys())[0] || "";

		$processSettingsStore.input.path = params.get('input_path') || $processSettingsStore.input.path
		$processSettingsStore.input.content =
				params.get('input_content') || $processSettingsStore.input.content
		$processSettingsStore.input.file_extension =
				(params.get('input_file_extension') as FileExtension) ||
				$processSettingsStore.input.file_extension

		$processSettingsStore.output.provider =
				(params.get('output_provider') as IOProvider) || $processSettingsStore.output.provider

		$processSettingsStore.output.provider_id ||=
				params.get('output_provider_id') || Array.from(outputAliases.keys())[0] || "";
		$processSettingsStore.output.path =
				params.get('output_path') || $processSettingsStore.output.path
		$processSettingsStore.output.content =
				params.get('output_content') || $processSettingsStore.output.content
		$processSettingsStore.output.file_extension =
				(params.get('output_file_extension') as OutputFileExtension) ||
				$processSettingsStore.output.file_extension

		if (hasFolderPicker($processSettingsStore.input.provider, true)) {
			setInputTree($processSettingsStore.input.provider, $processSettingsStore.input.provider_id, false)
		}

		if (hasFolderPicker($processSettingsStore.output.provider, true)) {
			setOutputTree($processSettingsStore.output.provider, $processSettingsStore.output.provider_id, false)
		}

		if (hasFolderPicker(fileStorage.provider, true)) {
			setFileUploadTree(fileStorage.provider, fileStorage.provider_id, false)
		}

		if ($processSettingsStore.input.provider === IO.File) {
			$processSettingsStore.input.path = ''
		}

		if ($processSettingsStore.input.provider !== IO.Text) {
			$processSettingsStore.input.content = ''
		}
	})

	let onCancelURL =
		$page.url.searchParams.get('from') || `/pipelines/${$processSettingsStore.pipeline_id}`

	let starting = false
	let uploading = false


	const uploadFiles = async () => {
		if (!files) return false

		uploading = true
		const formData = new FormData()

		for (const file of files) {
			formData.append('file', file, file.name)
		}

		const fileUpload = await fetch(
			`/api/files/upload?store=${fileStorage.storeFiles}&provider=${fileStorage.provider}&path=${fileStorage.path}&providerId=${fileStorage.provider_id}`,
			{
				method: 'POST',
				body: formData
			}
		)

		if (!fileUpload.ok) {
			const errorMessge = await fileUpload.text()
			toastStore.trigger(errorToast('File upload failed. ' + errorMessge + ' '))
			uploading = false
			return false
		}

		const json = await fileUpload.json()
		$processSettingsStore.input.path = json.path

		uploading = false
		return true
	}

	const createProcess = async () => {
		if (starting) {
			return
		}

		starting = true

		if (equals($processSettingsStore.input.provider, IO.File)) {
			const result = await uploadFiles()
			if (!result) {
				starting = false
				uploading = false
				return
			}
		}

		if (
			!$processSettingsStore.input.path.startsWith('/') &&
			equals($processSettingsStore.input.provider, IO.Dropbox) &&
			!isEmpty($processSettingsStore.input.path)
		) {
			$processSettingsStore.input.path = '/' + $processSettingsStore.input.path
		}

		if (
			!$processSettingsStore.output.path.startsWith('/') &&
			equals($processSettingsStore.output.provider, IO.Dropbox) &&
			!isEmpty($processSettingsStore.output.path)
		) {
			$processSettingsStore.output.path = '/' + $processSettingsStore.output.path
		}

		if ($processSettingsStore.input.provider !== IO.Text) {
			$processSettingsStore.input.content = ''
		}
		
		if ($processSettingsStore.output.provider === IO.None) {
			$processSettingsStore.output.path = ''
			$processSettingsStore.output.content = ''
		}

		const response = await fetch('/api/processes', {
			method: 'POST',
			body: JSON.stringify($processSettingsStore)
		})

		if (response.ok) {
			try {
				let process = await response.json()
				await goto(`/processes/${process.oid}`)
			} catch (err) {
				toastStore.trigger(errorToast(response.statusText))
				await goto(`/pipelines/${$processSettingsStore.pipeline_id}`)
			}
		} else {
			toastStore.trigger(errorToast((await response.text())))
			starting = false
		}

	}

	let fetchingTree = false

	const getFolderStructure = async (provider: IOProvider, providerId: string, isReset: boolean) => {
		
		if (!hasFolderPicker(provider, true) || !providerId || !$userSession?.connections[provider.toLowerCase()][providerId]) {
			return undefined;
		}
		
		fetchingTree = true
		
		let conns: ServiceConnections = $userSession?.connections[provider.toLowerCase()]

		if (!conns || !providerId) {
			toastStore.trigger(errorToast('No connections found for provider: ' + provider + " " + providerId))

			// alert(providerId + "\n" +  JSON.stringify($userSession?.connections[provider.toLowerCase()]))
			fetchingTree = false
			return undefined;
		}

        const response = await fetch('/api/processes/folderstructure',
				{
					method: 'POST',
					body: JSON.stringify({
						provider: provider, 
						user: $userSession?.oid, 
						reset: isReset, 
						providerId: providerId
					})
				})

		fetchingTree = false

		if (response.ok) {
			return (await response.json()) as TreeViewNode
		} else {
			toastStore.trigger(errorToast('Failed to fetch folder structure: ' + response.statusText))
		}

	}

	const setInputTree = async (provider: IOProvider, providerId: string, isReset: boolean) => {
		inputTree = await getFolderStructure(provider, providerId, isReset)	
	}
	
	const setOutputTree = async (provider: IOProvider, providerId: string, isReset: boolean) => {
		outputTree = await getFolderStructure(provider, providerId, isReset)
	}
	
	const setFileUploadTree = async (provider: IOProvider, providerId: string, isReset: boolean) => { 
		fileUploadTree = await getFolderStructure(provider, providerId, isReset)
	}

	let inputTree: TreeViewNode | undefined
	let outputTree: TreeViewNode | undefined
	let fileUploadTree: TreeViewNode | undefined

	
	$: inputAliases = hasConnections($processSettingsStore.input.provider) ? getCloudProviderAliases($userSession?.connections[$processSettingsStore.input.provider.toLowerCase()]) : new Map()
	$: outputAliases = hasConnections($processSettingsStore.output.provider) ? getCloudProviderAliases($userSession?.connections[$processSettingsStore.output.provider.toLowerCase()]) : new Map()
	$: fileUploadAliases = hasConnections(fileStorage.provider) ? getCloudProviderAliases($userSession?.connections[fileStorage.provider.toLowerCase()]) : new Map()
	

	$: inputBucketIsValid = isValidS3BucketName($processSettingsStore.input.path)
	$: outputBucketIsValid = isValidS3BucketName($processSettingsStore.output.path)
	$: settingsAreValid = areSettingsValid(
		$processSettingsStore.settings.worker_count,
		$processSettingsStore.settings.minimum_size
	)
	$: isValidFileStorage =
		$processSettingsStore.input.provider !== IO.File ||
		!fileStorage.storeFiles ||
		isValidFileUpload(fileStorage)
	$: uploadBucketIsValid = isValidS3BucketName(fileStorage?.path)
	$: $userSession

	let inputErrorPath = '';
	let outputErrorPath = '';
	let fileUploadErrorPath = '';
	
	$: fileUploadErrorPath = `/account?tab=1`;
	$: inputErrorPath  = `/account?tab=1`;
	$: outputErrorPath = `/account?tab=1`;

	// Error flags
	let isInputError  = false;
	let isOutputError = false;
	let isFileUploadError = false;


	$: if (!fileUploadTree &&  !isFileUploadError && fileStorage.provider_id && hasConnections(fileStorage.provider.toLowerCase())) {
		setFileUploadTree(
			fileStorage.provider,
			fileStorage.provider_id,
			false
		)
	}

	$: if (!inputTree && !isInputError && $processSettingsStore.input.provider_id && hasConnections($processSettingsStore.input.provider.toLowerCase())) {
		setInputTree(
			$processSettingsStore.input.provider,
			$processSettingsStore.input.provider_id,
			false
		)
	}

	$: if (!outputTree &&  !isOutputError && $processSettingsStore.output.provider_id && hasConnections($processSettingsStore.output.provider.toLowerCase())) {
		setOutputTree(
			$processSettingsStore.output.provider,
			$processSettingsStore.output.provider_id,
			false
		)
	}

	$: isInputError  =
		![IO.File, IO.Text, IO.None, IO.LocalDrive].includes($processSettingsStore.input.provider as IO) &&
		!hasConnections($processSettingsStore.input.provider.toLowerCase());

	$: isOutputError =
		![IO.File, IO.Text, IO.None, IO.LocalDrive].includes($processSettingsStore.output.provider as IO) &&
		!hasConnections($processSettingsStore.output.provider.toLowerCase());

	$: isFileUploadError = 
		equals($processSettingsStore.input.provider, IO.File) &&
		fileStorage.storeFiles &&
		(!equals(fileStorage.provider, IO.LocalDrive) && !hasConnections(fileStorage.provider.toLowerCase())) &&
		(equals(fileStorage.provider, IO.LocalDrive) && Object.keys(lfs || {}).length <= 0);

	$: isFileUploadRequirementsMet = (
		(!equals(fileStorage.provider, IO.LocalDrive) &&  !isEmpty(fileStorage.provider_id)) ||
		(equals(fileStorage.provider, IO.LocalDrive) && Object.keys(lfs || {}).length > 0)
	);

</script>

<svelte:head>
	<title>New Process</title>
</svelte:head>

<div class="menu-mobile">
	<button class="button-mobile" on:click={() => goto(onCancelURL)}>
		<Fa icon={faArrowLeft} />
		<span>Cancel</span>
	</button>
	<button
		class="button-mobile"
		disabled={starting ||
			!isValidFileStorage ||
			!isValidIO($processSettingsStore.input, $processSettingsStore.output, files, $userSession) ||
			!isEmpty(settingsAreValid)}
		on:click={createProcess}
	>
		<Fa icon={faCheck} />
		<span>Submit</span>
	</button>
</div>

<div class="h-full isolate">
	<div class="sticky top-0 bg-surface-50-900-token border-b border-color hidden md:block z-10">
		<div class="grid grid-cols-2 md:justify-between md:flex items-center relative">
			<button class="button-menu border-r border-color" on:click={() => goto(onCancelURL)}>
				<Fa icon={faArrowLeft} />
				<span>Cancel</span>
			</button>
			<button
				class="button-menu border-l border-color"
				disabled={starting ||
					!isValidFileStorage ||
					!isValidIO(
						$processSettingsStore.input,
						$processSettingsStore.output,
						files,
						$userSession
					) ||
					!isEmpty(settingsAreValid)}
				on:click={createProcess}
			>
				<Fa icon={faCheck} />
				<span>Submit</span>
			</button>
		</div>
	</div>
	<div class="p-4 md:p-16 space-y-8 pb-16">
		{#if uploading}
			<div class="w-full">
				<div class="section-wrapper p-8 space-y-8">
					<div class="flex-center-4 justify-between">
						<p>Uploading {files?.length || 0} files</p>

						<Fa icon={faFileArrowUp} size="3x" class="text-surface-300/20" />
					</div>
					<ProgressBar
						value={undefined}
						height="h-4"
						rounded="rounded-full"
						track="bg-surface-100-800-token"
						meter="variant-filled-primary"
						class="bordered-soft"
					/>
					<a href={`/pipelines/${$processSettingsStore.pipeline_id}?tab=1`} class="anchor-neutral">
						<Fa icon={faArrowLeft} />
						<span>Pipeline</span>
					</a>
				</div>
			</div>
		{:else if fetchingTree}

			<div class="h-full w-full flex items-center justify-center p-4">
				<div class="space-y-8 section-wrapper p-8 flex flex-col items-center">
					<h1 class="h2">Fetching Directory Structure...</h1>
					<hr class="hr" />
					<Fa icon={faRefresh} spin size="4x" />
					<p class="text-lg">Do not refresh this page</p>
				</div>
			</div>

		{:else}
			<div class="container mx-auto max-w-4xl grid gap-4">
				<div class="grid gap-4">
					<div
						class="section-wrapper space-y-4 p-4
				 {isValidInput($processSettingsStore.input, files, $userSession)
							? '!border-success-500 '
							: '!border-error-500'}"
					>
						<!-- INPUT -->
						<div class="flex-center-4 justify-between">
							<h2 class="h2">Input</h2>
							{#if isValidInput($processSettingsStore.input, files, $userSession)}
								<Fa icon={faCheck} class="text-success-500" size="2x" />
							{/if}
						</div>

						{#if isInputError || isFileUploadError}
							<div class="text-center w-full variant-soft-error p-4 rounded-md">
								<p class="mx-auto">
									{isInputError
										? 'To use ' + $processSettingsStore.input.provider + ' you must first connect your Account'
										: 'To use ' + fileStorage.provider + ' you must first connect your Account'
									} <a
									class="anchor"
									href={isFileUploadError ? fileUploadErrorPath : inputErrorPath}
									target="_blank">Account</a
								>
								</p>
							</div>
						{/if}

						<div class="grid gap-4">
							<div class="flex-center-4">
								<div class="flex-1">
									<Dropdown
										label="Source"
										options={IO_INPUT}
										bind:value={$processSettingsStore.input.provider}
										on:change={() => {
											$processSettingsStore.input.provider_id = ''
											$processSettingsStore.input.path = ''
											inputTree = undefined
										}}
									/>
								</div>
								{#if !isInputError && !equals($processSettingsStore.input.provider, IO.Text)}
									{#if !isStatelessProvider($processSettingsStore.input.provider) && hasConnections($processSettingsStore.input.provider.toLowerCase())}
										{#key $processSettingsStore.input.provider}
											<Dropdown
												label="Connection Alias"
												name="input-alias"
												options={inputAliases}
												initFirst={true}
												bind:value={$processSettingsStore.input.provider_id}
												on:change={() => {
													setInputTree(
														$processSettingsStore.input.provider,
														$processSettingsStore.input.provider_id,
														false
													)
												}}
											/>
										{/key}
									{/if}
									<Dropdown
										label="File extension"
										name="input-extension"
										options={INPUT_EXTENSIONS}
										bind:value={$processSettingsStore.input.file_extension}

									/>
								{/if}
							</div>

							<Dropdown
								label="Language"
								options={Languages}
								bind:value={$processSettingsStore.settings.language}
							/>

							{#if equals($processSettingsStore.input.provider, IO.Text)}
								<TextArea
									label="Document Text"
									name="content"
									error={$processSettingsStore.input.content === '' ? 'Text cannot be empty' : ''}
									bind:value={$processSettingsStore.input.content}
								/>
							{:else if equals($processSettingsStore.input.provider, IO.File)}
								<div class="space-y-1 ">
									<p class="form-label">File</p>
									<FileDropzone
										name="inputFile"
										bind:files
										accept={$processSettingsStore.input.file_extension}
										multiple={true}
										border="border border-color"
										rounded="rounded-md"
										class="input-wrapper"
									/>
									<p class="form-label {(files?.length || 0) === 0 ? 'text-error-500' : ''}">
										{files?.length || 0} files selected
									</p>
								</div>

								<Checkbox
									label="Upload input files to cloud storage."
									bind:checked={fileStorage.storeFiles}
								/>

								{#if fileStorage.storeFiles}
									<div class="grid gap-4"
										 class:grid-cols-1={!hasConnections(fileStorage.provider.toLowerCase())}
										 class:grid-cols-2={hasConnections(fileStorage.provider.toLowerCase())}
									>
										<Dropdown
											label="Provider"
											options={[IO.LocalDrive, IO.Dropbox, IO.Minio, IO.NextCloud, IO.Google]}
											bind:value={fileStorage.provider}
											on:change = {() => { fileStorage.provider_id = ""; fileUploadTree = undefined; }}
										/>
										{#if !isFileUploadError && hasConnections(fileStorage.provider.toLowerCase())}
											<Dropdown
												label="Connection Alias"
												options={fileUploadAliases}
												initFirst={true}

												bind:value={fileStorage.provider_id}
												on:change={() =>
													setFileUploadTree(
														fileStorage.provider,
														fileStorage.provider_id,
														false
													)
												}
											/>
										{/if}
									</div>
									{#if !isFileUploadError && isFileUploadRequirementsMet}
										{#if equals(fileStorage.provider, IO.Minio)}
											<TextInput
												label="Path (bucket/path/to/folder)"
												name="fileStoragePath"
												bind:value={fileStorage.path}
												error={uploadBucketIsValid}
											/>
										{:else if equals(fileStorage.provider, IO.LocalDrive)}
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
												<div class="w-1/12" >
													<span class="form-label text-start">Refresh</span>
													<button class="p-3 mt-1 ml-3 rounded-md hover:bg-primary-500 hover:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-opacity-50"
															on:click={() => setFileUploadTree(fileStorage.provider, fileStorage.provider_id, true)}>
														<Fa icon={faRepeat} />
													</button>
												</div>
											</div>
										{:else}
											<TextInput
												label="Path"
												name="fileStoragePath"
												bind:value={fileStorage.path}
												error={['/', ''].includes(fileStorage.path)
													? 'Writing to the root folder is not possible.'
													: ''}
											/>
										{/if}
									{/if}
								{/if}
							{:else if equals($processSettingsStore.input.provider, IO.LocalDrive) }
								{#if lfs && Object.keys(lfs).length > 0}
									<FolderStructure
											tree={lfs}
											label="Folder Picker"
											name="inputPaths"
											isMultiple={true}
											bind:value={$processSettingsStore.input.path}
									/>
								{:else}
									<Tip tipTheme="error" customIcon={faWarning}>
										The local drive is not available. Please select a different provider.
										To gain access to the local drive, please contact your administrator.
									</Tip>
								{/if}
							{:else if (!isInputError && !isEmpty($processSettingsStore.input.provider_id)) && equals($processSettingsStore.input.provider, IO.Minio)}
								<TextInput
									label="Path (bucket/path/to/folder)"
									error={inputBucketIsValid}
									name="inputPath"
									bind:value={$processSettingsStore.input.path}
								/>
							{:else if !isInputError && !isEmpty($processSettingsStore.input.provider_id)}
								<div class="flex w-full">
									{#if !inputTree}
										<div class="w">
											<TextInput
												label="Relative path"
												name="inputPath"
												bind:value={$processSettingsStore.input.path}
												error={$processSettingsStore.input.path === '/'
													? 'Provide an empty path to select the root folder.'
													: ''}
											/>
											<Tip>Do not include Apps/Docker Unified UIMA Interface in your path!</Tip>
										</div>
									{:else }
											<div class="w-11/12">
												<FolderStructure
													tree={inputTree}
													label="Folder Picker"
													name="inputPaths"
													isMultiple={true}
													bind:value={$processSettingsStore.input.path}
												/>
											</div>
											<div class="w-1/12" >
												<span class="form-label text-start">Refresh</span>
												<button class="p-3 mt-1 ml-3 rounded-md hover:bg-primary-500 hover:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-opacity-50"
														on:click={() => setInputTree($processSettingsStore.input.provider, $processSettingsStore.input.provider_id, true)}>
													<Fa icon={faRepeat} />
												</button>
											</div>
									{/if}
								</div>
							{/if}
						</div>

						<hr class="hr !w-full" />
						<div class="space-y-4">
							<h3 class="h3">Settings</h3>
							<div class="space-y-4">
								{#if !equals($processSettingsStore.input.provider, IO.Text)}
									<div class="grid grid-cols-2 gap-4 items-start">
										<div>
											<Number
												label="Minimum size"
												max={2147483647}
												name="skipFiles"
												help="All files with a size smaller than {$processSettingsStore.settings
													.minimum_size} bytes will not be processed."
												bind:value={$processSettingsStore.settings.minimum_size}
											/>
											<span class="text-xs pl-2">Bytes</span>
										</div>
										<Number
											label="Worker count"
											min={1}
											max={100}
											help="The number of threads used for processing. The actual number of threads is limited by the system."
											name="workerCount"
											bind:value={$processSettingsStore.settings.worker_count}
										/>
									</div>
								{/if}
								{#if !equals($processSettingsStore.input.provider, IO.Text) && !equals($processSettingsStore.input.provider, IO.File)}
									<Checkbox
										bind:checked={$processSettingsStore.settings.recursive}
										name="recursive"
										label="Find files recursively starting in the path directory"
									/>
								{/if}
								{#if !equals($processSettingsStore.input.provider, IO.Text)}
									<Checkbox
										bind:checked={$processSettingsStore.settings.sort_by_size}
										name="sortBySize"
										label="Sort files by size in ascending order"
									/>
								{/if}
								<Checkbox
									bind:checked={$processSettingsStore.settings.ignore_errors}
									name="ignoreErrors"
									label="Ignore errors encountered by documents and skip to the next available one."
								/>
							</div>
						</div>
					</div>

					<div
						class="section-wrapper p-4 space-y-4 flex flex-col justify-start relative
				 		{isValidOutput($processSettingsStore.output, $userSession)
							? '!border-success-500 '
							: '!border-error-500'}"
					>
						{#if isOutputError}
							<div class="text-center w-full variant-soft-error p-4 rounded-md">
								<p class="mx-auto">
									To use { $processSettingsStore.output.provider } you must first connect your <a
										class="anchor"
										href={ outputErrorPath }
										target="_blank">Account</a> and set an alias for your connection.
								</p>
							</div>
						{/if}

						<div class="flex-center-4 justify-between">
							<h2 class="h2">Output</h2>
							{#if isValidOutput($processSettingsStore.output, $userSession)}
								<Fa icon={faCheck} class="text-success-500" size="2x" />
							{/if}
						</div>
						<div class="space-y-4">
							<div class="flex-center-4">
								<div class="flex-1">
									<Dropdown
										label="Target"
										options={IO_OUTPUT}
										bind:value={$processSettingsStore.output.provider}
										on:change={() => {
											$processSettingsStore.output.provider_id = ''
											$processSettingsStore.output.path = ''
											outputTree = undefined
										}}
									/>
								</div>
								{#if !isOutputError && !isStatelessProvider($processSettingsStore.output.provider)}
									{#if hasConnections($processSettingsStore.output.provider.toLowerCase())}
										{#key $processSettingsStore.output.provider_id}
											<Dropdown
												label="Connection Alias"
												name="output-alias"
												options={outputAliases}
												initFirst={true}
												bind:value={$processSettingsStore.output.provider_id}
												on:change={() =>
													setOutputTree(
														$processSettingsStore.output.provider,
														$processSettingsStore.output.provider_id,
														false
													)
												}
											/>
										{/key}
									{/if}
									<Dropdown
										label="File extension"
										name="output-extension"
										options={OUTPUT_EXTENSIONS}
										bind:value={$processSettingsStore.output.file_extension}
									/>
								{/if}	
							</div>
							{#if equals($processSettingsStore.output.provider, IO.LocalDrive) }
								{#if Object.keys(lfs).length > 0}
									<FolderStructure
											tree={lfs}
											label="Folder Picker"
											name="outputPaths"
											isMultiple={true}
											bind:value={$processSettingsStore.output.path}
									/>
								{:else}
									<Tip tipTheme="error" customIcon={faWarning}>
										The local drive is not available. Please select a different provider.
										To gain access to the local drive, please contact your administrator.
									</Tip>
								{/if}
							{:else if !isOutputError && equals($processSettingsStore.output.provider, IO.Minio)}
								<TextInput
									label="Path (bucket/path/to/folder)"
									error={outputBucketIsValid}
									name="output-folder"
									bind:value={$processSettingsStore.output.path}
								/>
							{:else if !isOutputError && !isEmpty($processSettingsStore.output.provider_id) && hasFolderPicker($processSettingsStore.output.provider)}
								<div>
									{#if !outputTree }
										<TextInput
											label="Relative path"
											name="inputPath"
											bind:value={$processSettingsStore.output.path}
											error={$processSettingsStore.output.path === '/'
												? 'Provide an empty path to select the root folder.'
												: ''}
										/>
										<Tip>Do not include Apps/Docker Unified UIMA Interface in your path!</Tip>
									{:else}
										<div class="flex w-full">
											<div class="w-11/12">
												<FolderStructure
													tree={outputTree}
													label="Folder Picker"
													name="outputPaths"
													isMultiple={false}
													bind:value={$processSettingsStore.output.path}
												/>
											</div>
											<div class="w-1/12">
												<span class="form-label text-start">Refresh</span>
												<button class="p-3 mt-1 ml-3 rounded-md hover:bg-primary-500 hover:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-opacity-50"
														on:click={() => getFolderStructure($processSettingsStore.output.provider, $processSettingsStore.output.provider_id, true)}>
													<Fa icon={faRepeat} />
												</button>
											</div>
										</div>
									{/if}
								</div>
							{/if}
							{#if $processSettingsStore.output.provider !== IO.None}
								<hr class="hr !w-full" />
								<div class="space-y-4">
									<h3 class="h3">Settings</h3>
									<div class="space-y-4">
										{#if !equals($processSettingsStore.output.provider, IO.None)}
											<Checkbox
												bind:checked={$processSettingsStore.settings.check_target}
												name="checkTarget"
												label="Ignore files already present in the target location"
											/>
										{/if}

										{#if equals($processSettingsStore.output.provider, IO.Dropbox)}
											<Checkbox
												bind:checked={$processSettingsStore.settings.overwrite}
												name="overwrite"
												label="Overwrite existing files on conflict"
											/>
										{/if}
									</div>
								</div>
							{/if}
						</div>
						{#if $processSettingsStore.output.provider === IO.None}
							<div class="grow flex items-center justify-center flex-col gap-4 p-4">
								<div class="relative opacity-50">
									<Fa icon={faCloudUpload} size="8x" class="text-primary-500" />
									<Fa
										icon={faCloud}
										size="8x"
										class="top-1/2 left-1/2 -translate-x-[60%] -translate-y-[60%] absolute text-primary-500/50"
									/>
								</div>
								<p class="font-bold">Select an output location above</p>
							</div>
						{/if}
					</div>
				</div>
			</div>
		{/if}
	</div>
</div>
