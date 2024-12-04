<script lang="ts">
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
		isValidCloudProvider,
		getCloudProviderAliases,
		type FileExtension,
		type IOProvider
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
		faFileArrowUp, faRefresh, faRepeat
	} from '@fortawesome/free-solid-svg-icons'
	import Fa from 'svelte-fa'
	import { FileDropzone, ProgressBar, getToastStore, type TreeViewNode } from '@skeletonlabs/skeleton'
	import { onMount } from 'svelte'


	export let data
	const { user } = data

	const toastStore = getToastStore()

	let files: FileList

	let inputAliases: Record<string, string> = {}
	let outputAliases: Record<string, string> = {}

	let inputTree: TreeViewNode | null = null
	let outputTree: TreeViewNode | null = null
	let fetchingTree = false

	async function getFolderStructure(provider: IOProvider, isInput = true, isReset = false, providerId: string) {
		fetchingTree = true

		let tree: TreeViewNode | null = null

		if (!providerId || providerId === '') {
			fetchingTree = false
			return;
		}
		let aliases = isInput ? inputAliases : outputAliases

		const response = await fetch('/api/processes/folderstructure',
				{
					method: 'POST',
					body: JSON.stringify({provider: provider, user: $userSession?.oid, reset: isReset, providerId: aliases[providerId.toLowerCase()]})
				})

		if (response.ok) {
			tree = await response.json()
		}

		if (tree) {
			if (isInput) {
				inputTree = tree
			} else {
				outputTree = tree
			}
		}

		fetchingTree = false
	}

	function setAliases(isInput: boolean) {

		if (isInput) {
			inputAliases =
					!user.connections[$processSettingsStore.input.provider.toLowerCase()] ? {} :
							getCloudProviderAliases(user.connections[$processSettingsStore.input.provider.toLowerCase()])
		} else {
			outputAliases =
					!user.connections[$processSettingsStore.output.provider.toLowerCase()] ? {} :
							getCloudProviderAliases(user.connections[$processSettingsStore.output.provider.toLowerCase()])
		}
	}


	onMount(() => {
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
		$processSettingsStore.input.path = params.get('input_path') || $processSettingsStore.input.path
		$processSettingsStore.input.content =
			params.get('input_content') || $processSettingsStore.input.content
		$processSettingsStore.input.file_extension =
			(params.get('input_file_extension') as FileExtension) ||
			$processSettingsStore.input.file_extension


		if (isValidCloudProvider($processSettingsStore.input)) {
			setAliases(true)
			$processSettingsStore.input.provider_id = Object.keys(inputAliases)[0] || ""
			getFolderStructure($processSettingsStore.input.provider, true, false, $processSettingsStore.input.provider_id)
		}

		$processSettingsStore.output.provider =
			(params.get('output_provider') as IOProvider) || $processSettingsStore.output.provider
		$processSettingsStore.output.path =
			params.get('output_path') || $processSettingsStore.output.path
		$processSettingsStore.output.content =
			params.get('output_content') || $processSettingsStore.output.content
		$processSettingsStore.output.file_extension =
			(params.get('output_file_extension') as FileExtension) ||
			$processSettingsStore.output.file_extension

		if (isValidCloudProvider($processSettingsStore.output)) {
			setAliases(false)
			$processSettingsStore.output.provider_id = Object.keys(outputAliases)[0] || ""
			getFolderStructure($processSettingsStore.output.provider, false, false, $processSettingsStore.output.provider_id)
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

	let fileStorage = {
		storeFiles: false,
		provider: IO.Dropbox,
		path: '/upload'
	}

	const uploadFiles = async () => {
		if (!files) return false

		uploading = true
		const formData = new FormData()

		for (const file of files) {
			formData.append('file', file, file.name)
		}

		const fileUpload = await fetch(
			`/api/files/upload?store=${fileStorage.storeFiles}&provider=${fileStorage.provider}&path=${fileStorage.path}`,
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
			$processSettingsStore.input.path.length > 0
		) {
			$processSettingsStore.input.path = '/' + $processSettingsStore.input.path
		}

		if (
			!$processSettingsStore.output.path.startsWith('/') &&
			equals($processSettingsStore.output.provider, IO.Dropbox) &&
			$processSettingsStore.output.path.length > 0
		) {
			$processSettingsStore.output.path = '/' + $processSettingsStore.output.path
		}

		if ($processSettingsStore.input.provider !== IO.Text) {
			$processSettingsStore.input.content = ''
		}

		if ($processSettingsStore.input.provider_id !== "") {
			$processSettingsStore.input.provider_id = inputAliases[$processSettingsStore.input.provider_id]
		}

		if ($processSettingsStore.output.provider === IO.None) {
			$processSettingsStore.output.path = ''
			$processSettingsStore.output.content = ''
		}

		if ($processSettingsStore.output.provider_id !== "") {
			$processSettingsStore.output.provider_id = outputAliases[$processSettingsStore.output.provider_id]
		}

		const response = await fetch('/api/processes', {
			method: 'POST',
			body: JSON.stringify($processSettingsStore)
		})

		if (response.ok) {
			try {
				let process = await response.json()
				goto(`/processes/${process.oid}`)
			} catch (err) {
				toastStore.trigger(errorToast(response.statusText))
				goto(`/pipelines/${$processSettingsStore.pipeline_id}`)
			}
		} else {
			toastStore.trigger(errorToast(JSON.stringify(response.body)))
			starting = false
		}

	}

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
	$: uploadBucketIsValid = isValidS3BucketName(fileStorage.path)
	$: $userSession


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
			settingsAreValid.length !== 0}
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
					settingsAreValid.length !== 0}
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
						{#if $processSettingsStore.input.provider === IO.Minio &&
							Object.keys($userSession?.connections.minio).length <= 0}
							<div class="text-center w-full variant-soft-error p-4 rounded-md">
								<p class="mx-auto">
									To use Minio you must first connect your <a
										class="anchor"
										target="_blank"
										href="/account#minio">Account</a
									>
								</p>
							</div>
						{/if}

						{#if $processSettingsStore.input.provider === IO.Dropbox &&
							Object.keys($userSession?.connections.dropbox).length <= 0}
							<div class="text-center w-full variant-soft-error p-4 rounded-md">
								<p class="mx-auto">
									To use Dropbox you must first connect your <a
										class="anchor"
										href="/account#dropbox"
										target="_blank">Account</a
									>
								</p>
							</div>
						{/if}

						{#if $processSettingsStore.input.provider === IO.NextCloud &&
							Object.keys($userSession?.connections.nextcloud).length <= 0}
							<div class="text-center w-full variant-soft-error p-4 rounded-md">
								<p class="mx-auto">
									To use NextCloud you must first connect your <a
									class="anchor"
									href="/account#nextcloud"
									target="_blank">Account</a
								>
								</p>
							</div>
						{/if}

						{#if $processSettingsStore.input.provider === IO.Google &&
							Object.keys($userSession?.connections.google).length <= 0}
							<div class="text-center w-full variant-soft-error p-4 rounded-md">
								<p class="mx-auto">
									To use Google you must first connect your <a
									class="anchor"
									href="/account#google"
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
											if (!isValidCloudProvider($processSettingsStore.input)) return

											setAliases(true)
											$processSettingsStore.input.provider_id = Object.keys(inputAliases)[0]
											getFolderStructure(
													$processSettingsStore.input.provider,
													true,
													false,
													$processSettingsStore.input.provider_id
											)
										}}
									/>
								</div>
								{#if !equals($processSettingsStore.input.provider, IO.Text)}
									{#if $processSettingsStore.input.provider_id && isValidCloudProvider($processSettingsStore.input)}
										<Dropdown
											label="Connection Alias"
											name="input-alias"
											options={Object.keys(inputAliases) || [""]}
											bind:value={$processSettingsStore.input.provider_id}
											on:change={() =>
												getFolderStructure(
														$processSettingsStore.input.provider,
														true,
														false,
														$processSettingsStore.input.provider_id
												)}
										/>
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
								<div class="space-y-1">
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
								<!-- TODO: Add Other Providers-->
								<Checkbox
									label="Upload input files to cloud storage."
									bind:checked={fileStorage.storeFiles}
								/>
								{#if fileStorage.storeFiles}
									<div class="flex-1">
										<Dropdown
											label="Provider"
											options={[IO.Dropbox, IO.Minio]}
											bind:value={fileStorage.provider}
										/>
									</div>
									{#if equals(fileStorage.provider, IO.Minio)}
										<TextInput
											label="Path (bucket/path/to/folder)"
											name="fileStoragePath"
											bind:value={fileStorage.path}
											error={uploadBucketIsValid}
										/>
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
							{:else if equals($processSettingsStore.input.provider, IO.Minio)}
								<TextInput
									label="Path (bucket/path/to/folder)"
									error={inputBucketIsValid}
									name="inputPath"
									bind:value={$processSettingsStore.input.path}
								/>
							{:else}
								<div>
									{#if !inputTree}
										<TextInput
											label="Relative path"
											name="inputPath"
											bind:value={$processSettingsStore.input.path}
											error={$processSettingsStore.input.path === '/'
												? 'Provide an empty path to select the root folder.'
												: ''}
										/>
									{:else }
										<div class="flex w-full">
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
														on:click={() => getFolderStructure($processSettingsStore.input.provider, true, true, $processSettingsStore.input.provider_id)}>
													<Fa icon={faRepeat} />
												</button>
											</div>
										</div>
									{/if}
									<Tip>Do not include Apps/Docker Unified UIMA Interface in your path!</Tip>
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
						{#if $processSettingsStore.output.provider === IO.Minio &&
						Object.keys($userSession?.connections.minio).length <= 0}
							<div class="text-center w-full variant-soft-error p-4 rounded-md">
								<p class="mx-auto">
									To use Minio you must first connect your <a
										class="anchor"
										target="_blank"
										href="/account#minio">Account</a> and set an alias for your connection.
								</p>
							</div>
						{/if}

						{#if $processSettingsStore.output.provider === IO.Dropbox &&
							Object.keys($userSession?.connections.dropbox).length <= 0}
							<div class="text-center w-full variant-soft-error p-4 rounded-md">
								<p class="mx-auto">
									To use Dropbox you must first connect your <a
										class="anchor"
										href="/account#dropbox"
										target="_blank">Account</a> and set an alias for your connection.
								</p>
							</div>
						{/if}

						{#if $processSettingsStore.output.provider === IO.NextCloud &&
							Object.keys($userSession?.connections.nextcloud).length <= 0}
							<div class="text-center w-full variant-soft-error p-4 rounded-md">
								<p class="mx-auto">
									To use NextCloud you must first connect your <a
										class="anchor"
										href="/account#nextcloud"
										target="_blank">Account</a> and set an alias for your connection.
								</p>
							</div>
						{/if}

						{#if $processSettingsStore.output.provider === IO.Google &&
							Object.keys($userSession?.connections.google).length <= 0}
							<div class="text-center w-full variant-soft-error p-4 rounded-md">
								<p class="mx-auto">
									To use Google you must first connect your <a
										class="anchor"
										href="/account#google"
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
											if (!isValidCloudProvider($processSettingsStore.output)) return

											setAliases(false)
											$processSettingsStore.output.provider_id = Object.keys(outputAliases)[0]
											getFolderStructure($processSettingsStore.output.provider,
																false,
																false,
																$processSettingsStore.output.provider_id)

										}}

									/>
								</div>
								{#if $processSettingsStore.output.provider_id && isValidCloudProvider($processSettingsStore.output)}

									<Dropdown
										label="Connection Alias"
										name="output-alias"
										options={Object.keys(outputAliases) || [""]}
										bind:value={$processSettingsStore.output.provider_id}
										on:change={() =>
											getFolderStructure($processSettingsStore.output.provider,
																false,
																false,
																$processSettingsStore.output.provider_id)}
									/>
									<Dropdown
										label="File extension"
										name="output-extension"
										options={OUTPUT_EXTENSIONS}
										bind:value={$processSettingsStore.output.file_extension}
									/>
								{/if}
							</div>
							{#if equals($processSettingsStore.output.provider, IO.Minio)}
								<TextInput
									label="Path (bucket/path/to/folder)"
									error={outputBucketIsValid}
									name="output-folder"
									bind:value={$processSettingsStore.output.path}
								/>
							{:else if equals($processSettingsStore.output.provider, IO.Dropbox)
									   || equals($processSettingsStore.output.provider, IO.NextCloud)
									   || equals($processSettingsStore.output.provider, IO.Google)
							}
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
														on:click={() => getFolderStructure($processSettingsStore.output.provider, false, true, $processSettingsStore.output.provider_id)}>
													<Fa icon={faRepeat} />
												</button>
											</div>
										</div>
									{/if}
									<Tip>Do not include Apps/Docker Unified UIMA Interface in your path!</Tip>
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
