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
		type FileExtension,
		type OutputFileExtension,
		type IOProvider	} from '$lib/duui/io.js'
	import { Languages, blankSettings } from '$lib/duui/process'
	import { equals } from '$lib/duui/utils/text'
	import { errorToast } from '$lib/duui/utils/ui'
	import { processSettingsStore, userSession } from '$lib/store.js'
	import Checkbox from '$lib/svelte/components/Input/Checkbox.svelte'
	import Number from '$lib/svelte/components/Input/Number.svelte'
	import GeneralIoPanel from '$lib/process-io/GeneralIoPanel.svelte'
import {
	faArrowLeft,
	faCheck,
	faCloud,
	faCloudUpload,
	faFileArrowUp, faRefresh,
} from '@fortawesome/free-solid-svg-icons'
	import Fa from 'svelte-fa'
	import { ProgressBar, getDrawerStore, getToastStore } from '@skeletonlabs/skeleton'
	import { onMount } from 'svelte'
	import { writable } from 'svelte/store'
	import _ from 'lodash';
	const { set, isEmpty } = _;
	import { ProcessPageMode, ProcessPagePhase, type ProcessPageState, type ProcessPageStateStore } from '$lib/process-io/pageState'
	import PipelinePicker from './PipelinePicker.svelte'
	import {
		applyPipelineDraftIfAny,
		buildPayloadForPipeline,
		cloneProcessSettings,
		multiProcessQueueStore
	} from '$lib/duui/multiProcessQueue'
	import { get } from 'svelte/store'


	export let data
	const { user } = data

	$userSession = user

	const toastStore = getToastStore()
	const drawerStore = getDrawerStore()

	let inputPanel: any
	let outputPanel: any
	let inputValid: boolean = false
	let outputValid: boolean = false
	let inputValidFileStorage: boolean = true
	let uploadFilesCount: number = 0
	let multiPipelineIds: string[] = []

	const processPageState: ProcessPageStateStore = writable<ProcessPageState>({
		mode: ProcessPageMode.New,
		phase: ProcessPagePhase.Editing,
		uploadFilesCount: 0
	})

	// Helper: does this provider have any connections?
	function hasConnections(provider: string) {
		const key = provider.toLowerCase();
		return Object.keys($userSession?.connections[key] ?? {}).length > 0;
	}
	
			onMount(async () => {
				$userSession = user

		        try {

	            const params = $page.url.searchParams
				processPageState.update((s) => ({
					...s,
					mode:
						(params.get('from') || '').startsWith('/processes/')
							? ProcessPageMode.Restart
							: ProcessPageMode.New
				}))

				const initProcessSettingsFromParams = (params: URLSearchParams) => {
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

					$processSettingsStore.input.provider_id ||= params.get('input_provider_id') || ''

					$processSettingsStore.input.path = params.get('input_path') || $processSettingsStore.input.path
					$processSettingsStore.input.content =
						params.get('input_content') || $processSettingsStore.input.content
					$processSettingsStore.input.file_extension =
						(params.get('input_file_extension') as FileExtension) ||
						$processSettingsStore.input.file_extension

					$processSettingsStore.output.provider =
						(params.get('output_provider') as IOProvider) || $processSettingsStore.output.provider

					$processSettingsStore.output.provider_id ||= params.get('output_provider_id') || ''
					$processSettingsStore.output.path =
						params.get('output_path') || $processSettingsStore.output.path
					$processSettingsStore.output.content =
						params.get('output_content') || $processSettingsStore.output.content
					$processSettingsStore.output.file_extension =
						(params.get('output_file_extension') as OutputFileExtension) ||
						$processSettingsStore.output.file_extension
				}

				initProcessSettingsFromParams(params)
				inputPanel?.initFromParams?.(params)
				outputPanel?.initFromParams?.(params)
        } catch (err) {
            toastStore.trigger(errorToast('Error initializing process settings: ' + err))
        }
		})

	let onCancelURL =
		$page.url.searchParams.get('from') || `/pipelines/${$processSettingsStore.pipeline_id}`

	let starting = false
	let uploading = false


	const createProcess = async () => {
		if (starting) {
			return
		}

		starting = true

			const firstPipelineId = multiPipelineIds.at(0) || $processSettingsStore.pipeline_id
			if (multiPipelineIds.length > 0) {
				$processSettingsStore.pipeline_id = firstPipelineId
			}

			const result = await inputPanel?.uploadIfNeeded?.(firstPipelineId)
			if (!result) {
				starting = false
				processPageState.update((s) => ({ ...s, phase: ProcessPagePhase.Editing, uploadFilesCount: 0 }))
				return
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

		// Input provider-specific resets are handled inside `GeneralIoPanel`.
		
		if ($processSettingsStore.output.provider === IO.None) {
			$processSettingsStore.output.path = ''
			$processSettingsStore.output.content = ''
		}

		const isMulti = multiPipelineIds.length > 0

		if (isMulti) {
			const baseSettings = cloneProcessSettings($processSettingsStore)

			const existing = get(multiProcessQueueStore)
			multiProcessQueueStore.set({
				pipelineIds: [...multiPipelineIds],
				index: 0,
				baseSettings,
				configByPipelineId: existing?.configByPipelineId ?? {}
			})

			try {
				await applyPipelineDraftIfAny(firstPipelineId)
			} catch (err) {
				toastStore.trigger(errorToast('Failed to apply component configuration: ' + err))
				starting = false
				return
			}

			const response = await fetch('/api/processes', {
				method: 'POST',
				body: JSON.stringify(buildPayloadForPipeline(firstPipelineId, baseSettings))
			})

			if (response.ok) {
				try {
					let process = await response.json()
					// next index
					const q = get(multiProcessQueueStore)
					if (q) multiProcessQueueStore.set({ ...q, index: 1 })
					await goto(`/processes/${process.oid}`)
				} catch (err) {
					toastStore.trigger(errorToast(response.statusText))
					await goto(`/pipelines/${$processSettingsStore.pipeline_id}`)
				}
			} else {
				toastStore.trigger(errorToast((await response.text())))
				starting = false
			}

			return
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

	const openProcessConfiguration = (pipelineId: string) => {
		drawerStore.open({
			id: 'processConfiguration',
			position: 'right',
			rounded: 'rounded-none',
			border: 'border-l border-color',
			width: 'w-full lg:w-1/2 2xl:w-[40%]',
			meta: { pipelineId }
		})
	}

	let fetchingTree = false

	$: uploading = $processPageState.phase === ProcessPagePhase.Uploading
	$: fetchingTree = $processPageState.phase === ProcessPagePhase.DirectoryFetching
	$: uploadFilesCount = $processPageState.uploadFilesCount
	$: settingsAreValid = areSettingsValid(
		$processSettingsStore.settings.worker_count,
		$processSettingsStore.settings.minimum_size
	)
	
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

	$: isInputError  =
		![IO.File, IO.Text, IO.None, IO.LocalDrive].includes($processSettingsStore.input.provider as IO) &&
		!hasConnections($processSettingsStore.input.provider.toLowerCase());

	$: isOutputError =
		![IO.File, IO.Text, IO.None, IO.LocalDrive].includes($processSettingsStore.output.provider as IO) &&
		!hasConnections($processSettingsStore.output.provider.toLowerCase());
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
				!inputValidFileStorage ||
				!inputValid ||
				!outputValid ||
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
						!inputValidFileStorage ||
						!inputValid ||
						!outputValid ||
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
							<p>Uploading {uploadFilesCount || 0} files</p>

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
							<div class="section-wrapper p-4 space-y-4">
								<div class="flex items-center justify-between gap-4">
									<h2 class="h3">Pipelines</h2>
									<button
										type="button"
										class="button-neutral"
										on:click={() => openProcessConfiguration($processSettingsStore.pipeline_id)}
									>
										<span>Process configuration</span>
									</button>
								</div>
								<PipelinePicker bind:selectedIds={multiPipelineIds} />
							</div>
							<GeneralIoPanel
								bind:this={inputPanel}
								kind="input"
								label="Input"
								pageState={processPageState}
								errorPath={inputErrorPath}
								aliasName="input-alias"
								extensionName="input-extension"
								folderName="inputPaths"
								folderIsMultiple={true}
								providerOptions={IO_INPUT}
								extensionOptions={INPUT_EXTENSIONS}
								languageOptions={Languages}
								bind:language={$processSettingsStore.settings.language}
								bind:provider={$processSettingsStore.input}
								user={$userSession}
								bind:valid={inputValid}
								bind:validFileStorage={inputValidFileStorage}
							>
							<svelte:fragment slot="settings">
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
												<div class="space-y-2">
													<button
														type="button"
														class="button-neutral w-full !justify-between"
														on:click={() => openProcessConfiguration($processSettingsStore.pipeline_id)}
													>
														<span>Process configuration</span>
														<span class="text-xs dimmed">Workers + component options</span>
													</button>
													<p class="text-xs dimmed">
														Workers: {$processSettingsStore.settings.worker_count}
													</p>
												</div>
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
							</svelte:fragment>
						</GeneralIoPanel>

							<GeneralIoPanel
								bind:this={outputPanel}
								kind="output"
								label="Output"
								pageState={processPageState}
								errorPath={outputErrorPath}
								aliasName="output-alias"
							extensionName="output-extension"
							folderName="outputPaths"
							folderIsMultiple={false}
							providerOptions={IO_OUTPUT}
							extensionOptions={OUTPUT_EXTENSIONS}
							bind:provider={$processSettingsStore.output}
								user={$userSession}
								bind:valid={outputValid}
							>
							<svelte:fragment slot="settings">
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
							</svelte:fragment>
						</GeneralIoPanel>
				</div>
			</div>
		{/if}
	</div>
</div>
