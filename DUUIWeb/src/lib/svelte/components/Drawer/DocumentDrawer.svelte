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
		faDownload,
		faRefresh
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
	import Dropdown from "$lib/svelte/components/Input/Dropdown.svelte";

	const drawerStore = getDrawerStore()

	let _document: DUUIDocument = $drawerStore.meta.document
	let process: DUUIProcess = $drawerStore.meta.process
	let pipeline: DUUIPipeline = $drawerStore.meta.pipeline

	const input: DUUIDocumentProvider = process.input
	const output: DUUIDocumentProvider = process.output

	let URLIn: string = ''
	let URLOut: string = ''

	const toastStore = getToastStore()

	let annotationsExpanded: boolean = true
	let searchText: string = ''
	let minimumCount: number = 1
	let annotationFilter: Map<string, number> = new Map(Object.entries(_document.annotations || {}))
	let downloading: boolean = false

	const download = async () => {
		downloading = true
		const extraSlash = output.path.endsWith("/") ? "" : "/"
		const response = await fetch(
			`/api/files/download?provider=${output.provider}}&provider_id=${output.provider_id}&path=${output.path + extraSlash}${_document.name.replace(
				input.file_extension,
				output.file_extension
			)}`,
			{
				method: 'GET'
			}
		)

		if (response.ok) {
			const blob = await response.blob()
			const url = URL.createObjectURL(blob)
			const anchor = document.createElement('a')
			anchor.href = url
			anchor.download = _document.name.replace(input.file_extension, output.file_extension)
			document.body.appendChild(anchor)
			anchor.click()
			document.body.removeChild(anchor)
			URL.revokeObjectURL(url)
		} else {
			toastStore.trigger(errorToast(await response.text()))
		}

		downloading = false
	}

	type Annotation = {
		begin: number
		end: number
		annotationType: string
	}

	type ProcessedAnnotation = {
		text: string
		annotationType?: string
	}

	let selectedAnnotation: string = ""
	let documentText: string
	let annotationNames: string[]
	let unprocessedAnnotations: Annotation[]
	let processedAnnotations: ProcessedAnnotation[]
	let processingText = false

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
				.forEach(({ begin, end, annotationType }) => {
					if (currentIndex < begin) {
						parts.push({ text: text.slice(currentIndex, begin) });
					}
					parts.push({
						text: text.slice(begin, end),
						annotationType
					});
					currentIndex = end;
				});

		if (currentIndex < text.length) {
			parts.push({ text: text.slice(currentIndex) });
		}

		processingText = false

		return parts;
	}

	const preprocessDocument = async () => {
		processingText = true

		const extraSlash = output.path.endsWith("/") ? "" : "/"
		const filePath = `/api/files/preprocess?provider=${output.provider}&provider_id=${output.provider_id}&path=${output.path + extraSlash}${_document.name.replace(
				input.file_extension,
				output.file_extension
			)}&pipeline_id=${pipeline.oid}`

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
			} else {
				toastStore.trigger(errorToast(await response.text()))
			}
		}

		if (json) {

			documentText = json.text
			annotationNames = json.annotationNames
			unprocessedAnnotations = json.preprocessed


			selectedAnnotation = annotationNames[0]

			processedAnnotations = getHighlightedText(json.text, unprocessedAnnotations, selectedAnnotation)
		} else {
			toastStore.trigger(errorToast('Document not found'))
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

		preprocessDocument()
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
			{#if input.provider === IO.File}
				<p class="text-sm md:text-base max-w-[10ch]">
					{_document.name}
				</p>
			{:else}
				<p class="text-sm md:text-base">{_document.path}</p>
			{/if}
		</div>
		<div class="ml-auto justify-start items-center gap-4 flex">
			{#if output.provider !== IO.None && _document.status === Status.Completed}
				{#if downloading}
					<button class="button-neutral opacity-50">
						<Fa icon={faRefresh} spin />
						<span>Loading</span>
					</button>
				{:else}
					<button class="button-neutral" on:click={download}>
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
					<button class="button-neutral" on:click={preprocessDocument}>
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
			{#if output.provider !== IO.None && _document.status === Status.Completed}
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
				/>
				<hr class="border-t border-gray-300 my-4 rounded-md">
				<div class="p-8 py-4 border-b border-color flex justify-between items-center gap-8">
					<div class="flex flex-col items-start justify-center gap-2">
						<div class="h-64 overflow-y-auto">
							{#each processedAnnotations as part}
								{#if part.annotationType}
									<span class=" variant-soft-primary px-1 rounded"> {part.text}</span>
								{:else}
									<span>{part.text}</span>
								{/if}
							{/each}
						</div>
					</div>
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
