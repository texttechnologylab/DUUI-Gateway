<!--	
	@component
	A component that displays metrics and other information about a DUUIDocument
-->
<script lang="ts">
	import { IO, type DUUIDocument, type DUUIDocumentProvider } from '$lib/duui/io'
	import { Status } from '$lib/duui/monitor'
	import type { DUUIPipeline } from '$lib/duui/pipeline'
	import type { DUUIProcess } from '$lib/duui/process'
	import { formatFileSize, includes } from '$lib/duui/utils/text'
	import {errorToast, getStatusIcon, infoToast} from '$lib/duui/utils/ui'
	import { isDarkModeStore, userSession } from '$lib/store'
	import {
		faChevronDown,
		faClose,
		faDownload, faInfo,
		faRefresh,

		faWarning

	} from '@fortawesome/free-solid-svg-icons'
	import { getDrawerStore, getToastStore } from '@skeletonlabs/skeleton'
	import { onMount } from 'svelte'
	import Fa from 'svelte-fa'
	import {
		getAnnotationsPlotOptions,
		getTimelinePlotOptions
	} from '../../../../routes/processes/[oid]/chart'
	import Number from '../Input/Number.svelte'
	import Search from '../Input/Search.svelte'
	import Dropdown from '$lib/svelte/components/Input/Dropdown.svelte';
	import Link from '$lib/svelte/components/Link.svelte'
	import Tip from '../Tip.svelte'
	import IconTip from '../IconTip.svelte'

	const drawerStore = getDrawerStore()

	let _document: DUUIDocument = $drawerStore.meta.document
	let process: DUUIProcess = $drawerStore.meta.process
	let pipeline: DUUIPipeline = $drawerStore.meta.pipeline

	const input: DUUIDocumentProvider = process.input
	const output: DUUIDocumentProvider = process.output
	const hasOutputStorage = Boolean(output.provider && output.provider !== IO.None)

	let URLIn: string = ''
	let URLOut: string = ''

	const toastStore = getToastStore()

	let annotationsExpanded: boolean = true
	let searchText: string = ''
	let minimumCount: number = 1
	let annotationFilter: Map<string, number> = new Map(Object.entries(_document.annotations || {}))
	let downloading: boolean = false

	function getOutputName(
		name: string,
		inputExtension: string,
		outputExtension: string
	): string {
		if (!name) return name

		const inExt = inputExtension ?? ''
		const outExt = outputExtension ?? ''

		// If it already ends with the desired extension, keep it.
		if (outExt && name.endsWith(outExt)) return name

		// If it ends with the input extension, swap just that suffix.
		if (inExt && name.endsWith(inExt)) {
			return name.slice(0, -inExt.length) + outExt
		}

		// Fallback: replace the last extension segment, if any.
		if (outExt) {
			const dotIndex = name.lastIndexOf('.')
			if (dotIndex > 0) {
				return name.slice(0, dotIndex) + outExt
			}
		}

		return name
	}

	// Reactive full output path for display in the top bar
	$: fullOutputPath = (() => {
		const outputName = getOutputName(_document.name, input.file_extension, output.file_extension)

		// If we have an output path, append the (possibly rewritten) name
		if (output.path && outputName) {
			const extraSlash = output.path.endsWith('/') ? '' : '/'
			return `${output.path}${extraSlash}${outputName}`
		}

		// Fallbacks: use document path or name as stored
		return _document.path || outputName || _document.name
	})()

	const download = async () => {
		if (!hasOutputStorage) return
		downloading = true
		const extraSlash = output.path.endsWith('/') ? '' : '/'
		const outputName = getOutputName(_document.name, input.file_extension, output.file_extension)
		const downloadPath = `/api/files/download?provider=${output.provider}&provider_id=${output.provider_id}&path=${output.path + extraSlash}${outputName}`
		const response = await fetch(
			downloadPath,
			{
				method: 'GET'
			}
		)

		if (response.ok) {
			const blob = await response.blob()
			const url = URL.createObjectURL(blob)
			const anchor = document.createElement('a')
			anchor.href = url
			anchor.download = outputName
			document.body.appendChild(anchor)
			anchor.click()
			document.body.removeChild(anchor)
			URL.revokeObjectURL(url)
		} else {
			toastStore.trigger(errorToast(`Failed to download from ${output.provider}. \nEnsure that the document has been uploaded correctly.`))
			// toastStore.trigger(errorToast(await response.text()))
		}

		downloading = false
	}

	type Annotation = {
		begin: number
		end: number
		annotationType: string
		details: string
	}

	type ProcessedAnnotation = {
		text: string
		annotationType?: string
		details: string
	}

	let selectedAnnotation: string = ""
	let documentText: string
	let annotationNames: string[]
	let unprocessedAnnotations: Annotation[]
	let processedAnnotations: ProcessedAnnotation[]
	let processingText = false
	let activeAnnotation: ProcessedAnnotation | null = null

	let keyList: string[]

	function addKey(key: string, object: string) {
		while (true) {
			try {
				localStorage.setItem(key, object);
				// toastStore.trigger(successToast('Document is cached locally.'))
				break;
			} catch (e) {
				if (keyList.length > 0) {
					const removedKey = keyList.shift();
					localStorage.removeItem(removedKey);
				} else {
					toastStore.trigger(infoToast('Document cannot be cached locally.'))
					// localStorage.setItem("keyList", JSON.stringify([]));
					return;
				}
			}
		}

		keyList.push(key);
		localStorage.setItem("keyList", JSON.stringify(keyList));
	}

	function clearLocalStorage() {
		// keyList = JSON.parse(localStorage.getItem("keyList")) || [];
		keyList.forEach(key => localStorage.removeItem(key));
		localStorage.setItem("keyList", JSON.stringify(keyList));
	}

	function getHighlightedText(text: string, annotations: Annotation[], selectedType:string) {
		processingText = true

		const parts: ProcessedAnnotation[] = [];
		let currentIndex = 0;

		Object.values(annotations)
				.filter((annotation) => annotation.annotationType === selectedType)
				.forEach(({ begin, end, annotationType, details }) => {
					if (currentIndex < begin) {
						parts.push({ text: text.slice(currentIndex, begin), details });
					}
					parts.push({
						text: text.slice(begin, end),
						annotationType,
						details
					});
					currentIndex = end;
				});

		if (currentIndex < text.length) {
			parts.push({ text: text.slice(currentIndex), details: "" });
		}

		processingText = false

		return parts;
	}

	const preprocessDocument = async () => {
		if (!hasOutputStorage) {
			return
		}
		processingText = true
		const extraSlash = output.path.endsWith('/') ? '' : '/'
		const outputName = getOutputName(_document.name, input.file_extension, output.file_extension)
		const filePath = `/api/files/preprocess?provider=${output.provider}&provider_id=${output.provider_id}&path=${output.path + extraSlash}${outputName}&pipeline_id=${pipeline.oid}`

		let json: {
			text: string
			annotationNames: string[]
			preprocessed: Annotation[]
		} | null = null

		if (localStorage.getItem(filePath)) {
			const jsonString = localStorage.getItem(filePath)

			json = JSON.parse(jsonString) || null
		} else {
			const response = await fetch(filePath,
					{
						method: 'GET'
					}
			)
			if (response.ok) {
				json = await response.json()
				addKey(filePath, JSON.stringify(json))
			}
		}

		if (json) {

			documentText = json.text
			annotationNames = json.annotationNames
			unprocessedAnnotations = json.preprocessed


			selectedAnnotation = annotationNames[0]

			processedAnnotations = getHighlightedText(json.text, unprocessedAnnotations, selectedAnnotation)
		} else {
			toastStore.trigger(errorToast(`Failed to download from ${output.provider}. \nEnsure that the document has been uploaded correctly.`))
		}

		processingText = false
	}

	switch (input.provider) {
		case IO.Dropbox:
			URLIn = 'https://www.dropbox.com/home/Apps/DUUI'
			break
		case IO.Minio:
			URLIn = $userSession?.connections.minio[input.provider_id].endpoint || ''
			break
		default:
			URLIn = ''
	}

	switch (output.provider) {
		case IO.Dropbox:
			URLOut = 'https://www.dropbox.com/home/Apps/DUUI'
			break
		case IO.Minio:
			URLOut = $userSession?.connections.minio[output.provider_id].endpoint || ''
			break
		default:
			URLOut = ''
	}

	let ApexCharts
	let loaded: boolean = false

	const chart = (node: HTMLDivElement, options: any) => {
		if (!loaded) return

		let _chart = new ApexCharts(node, options)
		_chart.render()

		return {
			update(options: any) {
				_chart.updateOptions(options)
			},
			destroy() {
				_chart.destroy()
			}
		}
	}

	let options
	let eventOptions

	$: {
		options = getAnnotationsPlotOptions(annotationFilter, $isDarkModeStore)
		eventOptions = getTimelinePlotOptions(process, pipeline, _document, $isDarkModeStore)
	}

	function handleBeforeUnload(event: BeforeUnloadEvent) {
		if (keyList.length > 0) {
			clearLocalStorage()
		}
	}

	onMount(() => {
		async function loadApexCharts() {
			const module = await import('apexcharts')
			ApexCharts = module.default
			window.ApexCharts = ApexCharts
			loaded = true
		}
		window.addEventListener('beforeunload', handleBeforeUnload);

		keyList = JSON.parse(localStorage.getItem("keyList") || "[]")

		loadApexCharts()

		if (hasOutputStorage) {
			preprocessDocument()
		}
	})

	$: {
		if (_document.annotations) {
			annotationFilter = new Map(
				Object.entries(_document.annotations).filter(
					(entry) =>
						(includes(entry[0], searchText) || searchText === '') && entry[1] >= minimumCount
				)
			)
		}
	}
</script>

<div class="h-full bg-surface-50-900-token">
	<div id="scroll-top" />
	<div
		class="w-full z-50 grid
		font-bold text-2xl p-4 border-surface-200 dark:border-surface-500 sm:flex items-center justify-start gap-4 sticky top-0 bg-surface-100-800-token border-b border-color"
	>
			<div class="flex-center-4">
				<Fa icon={getStatusIcon(_document.status)} size="lg" class="dimmed" />
				<p class="text-sm md:text-base break-all">
					{fullOutputPath}
				</p>
			</div>
		<div class="ml-auto justify-start items-center gap-4 flex">
			{#if _document.status === Status.Completed}
				{#if downloading}
					<button class="button-neutral opacity-50">
						<Fa icon={faRefresh} spin />
						<span>Loading</span>
					</button>
				{:else}
					<!-- TODO fix tooltip trigger-->
					<!-- <TooltipTrigger
						tooltipVisible={!hasOutputStorage}
						tooltipMessage="Configure an output storage to download processed documents."
						tipTheme="tertiary"
						tipIcon={faWarning}
					>
						<button class="button-neutral" on:click={download} disabled={!hasOutputStorage}>
							<Fa icon={faDownload} />
							<span>Download</span>
						</button>
					</TooltipTrigger> -->
					<button class="button-neutral" on:click={download} disabled={!hasOutputStorage}>
						<Fa icon={faDownload} />
						<span>Download</span>
					</button>
				{/if}
				{#if processingText}
					<button class="button-neutral opacity-50">
						<Fa icon={faRefresh} spin />
						<span>Processing Document</span>
					</button>
				{:else}
					<!-- <TooltipTrigger
						tooltipVisible={!hasOutputStorage}
						tooltipMessage="Configure an output storage to download processed documents."
						tipTheme="tertiary"
						tipIcon={faWarning}
					>
						<button class="button-neutral" on:click={preprocessDocument} disabled={!hasOutputStorage}>
							<Fa icon={faDownload} />
							<span>Read Document</span>
						</button>
					</TooltipTrigger> -->
					<button class="button-neutral" on:click={preprocessDocument} disabled={!hasOutputStorage}>
						<Fa icon={faDownload} />
						<span>Read Document</span>
					</button>
				{/if}
			{/if}
			<button class="button-neutral" on:click={drawerStore.close}>
				<Fa icon={faClose} />
				<span>Close</span>
			</button>
		</div>
	</div>

	<div class="bg-surface-50-900-token">
		{#if _document.error}
			<div class="p-4 grid justify-center text-center text-lg variant-soft-error font-bold gap-8">
				<h3 class="h3">The document has encountered an error</h3>
				<p class="mx-auto">
					{_document.error}
				</p>
			</div>
		{/if}
		<div
			class="p-4 grid grid-cols-2 md:grid-cols-4 gap-4 justify-center items-center border-b border-color text-sm md:text-base"
		>
			<div class="flex flex-col items-start justify-center gap-2">
				<p class="font-bold">Status</p>
				<p>{_document.status}</p>
			</div>
			<div class="flex flex-col items-start justify-center gap-2">
				<p class="font-bold">Size</p>
				<p>{_document.size ? formatFileSize(_document.size) : 'Unknown'}</p>
			</div>
			{#if URLIn}
				<a href={URLIn} target="_blank" class="flex flex-col items-start justify-center gap-2">
					<p class="anchor font-bold">Source</p>
					<p>{input.provider}</p>
				</a>
			{:else}
				<div class="flex flex-col items-start justify-center gap-2">
					<p class="font-bold">Source</p>
					<p>{input.provider}</p>
				</div>
			{/if}
			{#if hasOutputStorage && _document.status === Status.Completed}
				<a href={URLOut} target="_blank" class="flex flex-col items-start justify-center gap-2">
					<p class="anchor font-bold">Target</p>
					<p>{output.provider}</p>
				</a>
			{:else}
				<div class="flex flex-col items-start justify-center gap-2">
					<p class="font-bold">Target</p>
					<p>{output.provider}</p>
				</div>
			{/if}
		</div>
		<!-- <div
			class="p-4 grid-cols-2 grid md:grid-cols-4 gap-4 justify-center items-center border-b border-color text-sm md:text-base"
		>
			<div class="flex flex-col items-start justify-center gap-2">
				<p class="font-bold">Setup</p>
				<p>{formatMilliseconds(_document.duration_wait)}</p>
			</div>
			<div class="flex flex-col items-start justify-center gap-2">
				<p class="font-bold">Wait</p>
				<p>{formatMilliseconds(_document.duration_wait)}</p>
			</div>

			<div class="flex flex-col items-start justify-center gap-2">
				<p class="font-bold">Process</p>
				<p>{formatMilliseconds(_document.duration_process)}</p>
			</div>
			<div class="flex flex-col items-start justify-center gap-2">
				<p class="font-bold">Total</p>
				<p>{formatMilliseconds(_document.duration || 0)}</p>
			</div>
		</div> -->
		{#if selectedAnnotation}
			<div class="p-4 flex flex-col gap-4">
				<h2 class="h2">Document</h2>
				<Dropdown
						name="annotationNames"
						label="Annotations"
						placement="bottom-start"
						offset={0}
						bind:value={selectedAnnotation}
						on:change={() =>
							processedAnnotations = getHighlightedText(documentText, unprocessedAnnotations, selectedAnnotation)}
						style=""
						options={annotationNames}
						searchable={true}
						searchPlaceholder="Search annotations..."
				/>
				<hr class="border-t border-gray-300 my-4 rounded-md">
				<div class="p-8 py-4 border-b border-color flex gap-8">
					<div class={`flex-grow ${activeAnnotation ? 'w-3/4' : 'w-full'} space-y-1 relative`}>
						<IconTip
							text="Click highlighted sections to view annotation details."
							size="xl"
							customTipSize={true}
							tipSize="sm"
							followCursor={true}
						/>
						<div class={`h-96 overflow-y-auto leading-relaxed `}>
							{#each processedAnnotations as part}
								{#if part.annotationType }
									<button
										type="button"
										class={`variant-soft-primary px-1 rounded cursor-pointer transition
											hover:brightness-110 focus:outline-none focus:ring-2 focus:ring-primary-500
											${activeAnnotation === part ? 'ring-2 ring-primary-500 font-semibold' : ''}`}
										on:click={() => (activeAnnotation = activeAnnotation === part ? null : part)}
									>
										{part.text}
									</button>
								{:else}
									<span>{part.text}</span>
								{/if}
							{/each}
						</div>
					</div>

					{#if activeAnnotation}
						<div class="w-1/4 border border-color rounded text-sm">
							<!-- Header -->
							<div class="flex items-center justify-between px-2 py-1 border-b border-color">
								<p class="font-semibold truncate mr-2">
									{activeAnnotation.annotationType
									? activeAnnotation.annotationType.split('.').slice(-1)
									: 'Details'}
								</p>

								<IconTip
									text="Click the highlighted text again to hide details."
									size="lg"
									customTipSize={true}
									tipSize="xs"
									tipTheme="primary"
									followCursor={true}
								/>
							</div>

							<!-- Content -->
							<div class="max-h-96 overflow-y-auto p-2">
								<pre class="whitespace-pre-wrap break-words text-xs">
									{activeAnnotation.details}
								</pre>
							</div>
						</div>
					{/if}
				</div>
			</div>
			<hr class="border-t border-gray-300 my-4 rounded-md">
		{/if}

		<div class="p-4 flex flex-col gap-4 border-b border-color">
			{#if _document.annotations && Object.entries(_document.annotations).length > 0}
				<h2 class="h2">Annotations</h2>
				<div class="flex items-end gap-4 justify-end">
					<button
						class="button-neutral mr-auto"
						on:click={() => (annotationsExpanded = !annotationsExpanded)}
					>
						<div class:turn={annotationsExpanded} class="transition-transform duration-300">
							<Fa icon={faChevronDown} size="lg" />
						</div>
						<span>{annotationsExpanded ? 'Collapse' : 'Expand'}</span>
					</button>
					<div class="grid md:grid-cols-2 items-end gap-4">
						<Number label="Minimum count" name="minimumCount" bind:value={minimumCount} />
						<Search placeholder="Search" bind:query={searchText} />
					</div>
				</div>

				<div class:open={annotationsExpanded} class="content dimmed">
					<div class="content-wrapper space-y-4">
						<div
							class="self-stretch text-sm overflow-hidden grid sm:grid-cols-2 xl:grid-cols-3 gap-4"
						>
							{#each annotationFilter.entries() as [key, value]}
								<div class="grid grid-cols-2 card gap-4 p-4">
									<div class="flex flex-col items-start justify-center gap-2">
										<p class="font-bold">Class</p>
										<p class="">{key.split('.').slice(-1)}</p>
									</div>
									<div class="flex flex-col items-start justify-center gap-2">
										<p class="font-bold">Count</p>
										<p>{value}</p>
									</div>
								</div>
							{/each}
						</div>
					</div>
				</div>
				{#if loaded}
					<div use:chart={options} />
				{/if}
			{/if}
			{#if loaded && _document.events}
				<h2 class="h2">Timeline</h2>
				<div class="pr-4" use:chart={eventOptions} />
			{/if}
		</div>
	</div>
</div>

<style>
	.content-wrapper {
		overflow: hidden;
	}

	.content {
		display: grid;
		grid-template-rows: 0fr;
		transition: grid-template-rows 300ms;
	}

	.open {
		grid-template-rows: 1fr;
	}

	.turn {
		transform: rotate(-180deg);
	}
</style>
