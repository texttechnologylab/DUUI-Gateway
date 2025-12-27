<!--
	@component
	Pipeline picker dropdown with search, add button, and edit button (opens ProcessConfiguration drawer).
-->
<script lang="ts">
	import { popup, type PopupSettings, getDrawerStore, getToastStore } from '@skeletonlabs/skeleton'
	import { includes } from '$lib/duui/utils/text'
	import { errorToast } from '$lib/duui/utils/ui'
	import type { DUUIPipeline } from '$lib/duui/pipeline'
	import Fa from 'svelte-fa'
	import { faChevronDown, faPlus, faPenToSquare } from '@fortawesome/free-solid-svg-icons'
	import { onMount } from 'svelte'
	import { v4 as uuidv4 } from 'uuid'

	export let label = 'Pipelines'
	export let selectedIds: string[] = []

	let pipelines: DUUIPipeline[] = []
	let loading = false
	let search = ''

	const drawerStore = getDrawerStore()
	const toastStore = getToastStore()

	const dropdownId = uuidv4()
	const popupName = `pipeline-picker-${dropdownId}`

	const dropdown: PopupSettings = {
		event: 'click',
		target: popupName,
		placement: 'bottom-start',
		closeQuery: `.close-${dropdownId}`,
		middleware: { offset: 4 }
	}

	const load = async () => {
		loading = true
		try {
			const res = await fetch('/api/pipelines/batch?limit=200', { method: 'GET' })
			if (!res.ok) {
				toastStore.trigger(errorToast(await res.text()))
				return
			}
			const json = await res.json()
			pipelines = (json.pipelines ?? []) as DUUIPipeline[]
		} finally {
			loading = false
		}
	}

	onMount(load)

	$: filtered =
		!search
			? pipelines
			: pipelines.filter((p) => includes(`${p.name} ${p.description}`, search))

	const add = (id: string) => {
		if (selectedIds.includes(id)) return
		selectedIds = [...selectedIds, id]
	}

	const edit = (id: string) => {
		drawerStore.open({
			id: 'processConfiguration',
			position: 'right',
			rounded: 'rounded-none',
			border: 'border-l border-color',
			width: 'w-full lg:w-1/2 2xl:w-[40%]',
			meta: { pipelineId: id }
		})
	}

	$: labelText =
		selectedIds.length === 0
			? 'Select pipelines'
			: `${selectedIds.length} selected`
</script>

<div class="label flex flex-col">
	<span class="form-label">{label}</span>
	<button class="flex items-center !justify-between gap-2 px-3 py-2 leading-6 border border-color rounded-md input-wrapper"
		use:popup={dropdown}
		aria-haspopup="listbox"
	>
		<span class="truncate">{labelText}</span>
		<Fa icon={faChevronDown} />
	</button>
</div>

<div data-popup={popupName} class="fixed mt-1 z-20">
	<div class="popup-solid p-2 min-w-[320px] max-w-[480px]">
		<div class="mb-2">
			<input
				type="text"
				class="w-full px-2 py-1 text-sm border border-color rounded-md bg-surface-50-900-token"
				placeholder="Search pipelines..."
				bind:value={search}
				on:click|stopPropagation
			/>
		</div>

		{#if loading}
			<div class="p-2 text-sm dimmed">Loading…</div>
		{:else}
			<div class="max-h-96 overflow-y-auto space-y-2">
				{#each filtered as pipeline (pipeline.oid)}
					<div class="section-wrapper !shadow-none !border p-3 flex items-center justify-between gap-3">
						<div class="min-w-0">
							<p class="font-bold truncate">{pipeline.name}</p>
							<p class="text-xs dimmed truncate">{pipeline.description}</p>
						</div>
						<div class="flex items-center gap-2">
							<button
								class={`button-neutral !px-3 ${`close-${dropdownId}`}`}
								on:click={() => edit(pipeline.oid)}
							>
								<Fa icon={faPenToSquare} />
							</button>
							<button
								class={`button-neutral !px-3 ${`close-${dropdownId}`}`}
								disabled={selectedIds.includes(pipeline.oid)}
								on:click={() => add(pipeline.oid)}
							>
								<Fa icon={faPlus} />
							</button>
						</div>
					</div>
				{/each}
			</div>
		{/if}
	</div>
</div>

