<!--
	@component
	A drawer to configure process-level settings (worker_count) and edit component options for the selected pipeline.
-->
<script lang="ts">
	import { getDrawerStore, getToastStore } from '@skeletonlabs/skeleton'
	import { errorToast, successToast } from '$lib/duui/utils/ui'
	import type { DUUIPipeline } from '$lib/duui/pipeline'
	import { processSettingsStore } from '$lib/store'
	import { multiProcessQueueStore, type PipelineProcessConfigurationDraft } from '$lib/duui/multiProcessQueue'
	import { v4 as uuidv4 } from 'uuid'
	import { onMount } from 'svelte'
	import { get } from 'svelte/store'
	import Fa from 'svelte-fa'
	import { faClose, faSave } from '@fortawesome/free-solid-svg-icons'
	import Number from '$lib/svelte/components/Input/Number.svelte'
	import ComponentOptions from '$lib/svelte/components/Input/ComponentOptions.svelte'
	import Tip from '$lib/svelte/components/Tip.svelte'

	const drawerStore = getDrawerStore()
	const toastStore = getToastStore()

	const pipelineId: string = $drawerStore.meta.pipelineId

	let pipeline: DUUIPipeline | null = null
	let loading = false
	let saving = false
	let workerCount: number = $processSettingsStore.settings.worker_count

	const loadPipeline = async () => {
		loading = true
		try {
			const res = await fetch(`/api/pipelines?id=${pipelineId}`, { method: 'GET' })
			if (!res.ok) {
				toastStore.trigger(errorToast(await res.text()))
				pipeline = null
				return
			}

			const p: DUUIPipeline = await res.json()
			p.components?.forEach((c) => {
				// backend doesn't store UI drag/drop id
				;(c as any).id ||= uuidv4()
			})
			pipeline = p

			const q = get(multiProcessQueueStore)
			const existing = q?.configByPipelineId?.[pipelineId]
			if (existing) {
				workerCount = existing.worker_count
				pipeline.components = existing.components
			}
		} finally {
			loading = false
		}
	}

	onMount(loadPipeline)

	const save = async () => {
		if (!pipeline || saving) return
		saving = true
		try {
			const current = get(multiProcessQueueStore)

			const draft: PipelineProcessConfigurationDraft = {
				pipelineId,
				worker_count: workerCount,
				components: pipeline.components ?? []
			}

			multiProcessQueueStore.set({
				pipelineIds: current?.pipelineIds ?? [],
				index: current?.index ?? 0,
				baseSettings: current?.baseSettings ?? $processSettingsStore,
				configByPipelineId: {
					...(current?.configByPipelineId ?? {}),
					[pipelineId]: draft
				}
			})

			toastStore.trigger(successToast('Saved locally'))
			drawerStore.close()
		} finally {
			saving = false
		}
	}
</script>

<div class="space-y-4 h-full bg-surface-100-800-token">
	<div class="flex items-stretch gap-4 justify-between border-b border-color bg-surface-50-900-token">
		<button class="button-menu border-r border-color" disabled={saving || loading} on:click={save}>
			<Fa icon={faSave} />
			<span>{saving ? 'Saving' : 'Save'}</span>
		</button>
		<p class="text-lg self-center">Process configuration</p>
		<button class="button-menu border-l border-color" on:click={drawerStore.close}>
			<Fa icon={faClose} />
			<span>Close</span>
		</button>
	</div>

	<div class="p-4 space-y-6">
			<div class="section-wrapper p-4 space-y-3">
				<h3 class="h3">Workers</h3>
				<Number
					label="Worker count"
					min={1}
					max={100}
					help="The number of threads used for processing. The actual number of threads is limited by the system."
					name="workerCount"
					bind:value={workerCount}
				/>
			</div>

		{#if loading}
			<div class="section-wrapper p-4">
				<p>Loading pipeline…</p>
			</div>
		{:else if !pipeline}
			<Tip tipTheme="error" tipSize="lg">Failed to load pipeline.</Tip>
		{:else}
			<div class="section-wrapper p-4 space-y-4">
				<div class="flex items-center justify-between gap-4">
					<h3 class="h3 truncate">Components · {pipeline.name}</h3>
					<button class="button-neutral" disabled={loading} on:click={loadPipeline}>
						<span>Refresh</span>
					</button>
				</div>
				<div class="space-y-6">
					{#each pipeline.components as component (component.oid)}
						<div class="card p-4 space-y-4">
							<div class="flex items-center justify-between gap-4">
								<div class="min-w-0">
									<p class="font-bold truncate">{component.name}</p>
									<p class="text-xs dimmed truncate">{component.driver}</p>
								</div>
							</div>
							<ComponentOptions {component} />
						</div>
					{/each}
				</div>
			</div>
		{/if}
	</div>
</div>
