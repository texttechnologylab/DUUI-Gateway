```ts
// Sketch (code-first): multi-process queue + process configuration drawer
// Repo reality:
// - Process create/cancel/etc is via SvelteKit endpoints in `DUUIWeb/src/routes/api/processes/*`
// - Processes page uses `ProcessPageState` in `DUUIWeb/src/lib/process-io/pageState.ts`
// - Dropdown w/ search exists: `DUUIWeb/src/lib/svelte/components/Input/Dropdown.svelte`
// - Component card style exists: `DUUIWeb/src/lib/svelte/components/PipelineComponent.svelte`
// - Drawer pattern exists: `DUUIWeb/src/routes/processes/[oid]/ProcessDrawer.svelte` and `DUUIWeb/src/lib/svelte/components/Drawer/ComponentDrawer.svelte`
```

```ts
// DUUIWeb/src/lib/process-io/pageState.ts
export enum ProcessRunMode {
	Single = 'Single',
	Multi = 'Multi'
}

export type ProcessPageState = {
	mode: ProcessPageMode
	runMode: ProcessRunMode
	phase: ProcessPagePhase
	uploadFilesCount: number
}
```

```svelte
<!-- DUUIWeb/src/routes/processes/+page.svelte (layout + toggle + panel placement) -->
<!-- XL: left column panel; smaller: panel above IO panels -->

<div class="container mx-auto max-w-4xl grid gap-4">
	<div class="grid gap-4">
		<div class="section-wrapper p-4 flex items-center justify-between">
			<h2 class="h3">Run mode</h2>
			<div class="flex">
				<button class="button-neutral" on:click={() => processPageState.update(s => ({...s, runMode: ProcessRunMode.Single }))}>
					Single
				</button>
				<button class="button-neutral" on:click={() => processPageState.update(s => ({...s, runMode: ProcessRunMode.Multi }))}>
					Multi
				</button>
			</div>
		</div>

		<div class="grid gap-4 xl:grid-cols-[minmax(360px,420px)_1fr]">
			{#if $processPageState.runMode === ProcessRunMode.Multi}
				<MultiSelectionPanel />
			{/if}
			<div class="grid gap-4">
				<GeneralIoPanel kind="input" ... />
				<GeneralIoPanel kind="output" ... />
			</div>
		</div>
	</div>
</div>
```

```ts
// DUUIWeb/src/lib/duui/processConfiguration.ts (new)
export interface ProcessConfiguration {
	worker_count: number
}
```

```ts
// DUUIWeb/src/lib/store.ts (new store, matches existing store style)
import { writable, type Writable } from 'svelte/store'
import type { ProcessSettings } from './duui/process'
import type { ProcessConfiguration } from './duui/processConfiguration'

export const multiProcessQueueStore: Writable<{
	pipelineIds: string[]
	index: number
	baseSettings: ProcessSettings
	baseConfiguration: ProcessConfiguration
} | null> = writable(null)
```

```svelte
<!-- DUUIWeb/src/routes/processes/MultiSelectionPanel.svelte (new) -->
<script lang="ts">
	import { onMount } from 'svelte'
	import { getToastStore } from '@skeletonlabs/skeleton'
	import { errorToast } from '$lib/duui/utils/ui'
	import type { DUUIPipeline } from '$lib/duui/pipeline'
	import { includes } from '$lib/duui/utils/text'
	import Fa from 'svelte-fa'
	import { faPlus, faTrash, faSearch } from '@fortawesome/free-solid-svg-icons'

	export let selected: DUUIPipeline[] = []

	let all: DUUIPipeline[] = []
	let q = ''

	const toastStore = getToastStore()

	onMount(async () => {
		const res = await fetch('/api/pipelines/batch?limit=200', { method: 'GET' })
		if (!res.ok) return toastStore.trigger(errorToast(await res.text()))
		const json = await res.json()
		all = json.pipelines ?? []
	})

	$: filtered = all.filter((p) => includes(`${p.name} ${p.description}`, q))
</script>

<div class="section-wrapper p-4 space-y-4">
	<h3 class="h3">Multi selection</h3>

	<div class="space-y-2">
		<div class="flex items-center gap-2">
			<Fa icon={faSearch} class="dimmed" />
			<input class="input w-full" placeholder="Search pipelines..." bind:value={q} />
		</div>

		<div class="max-h-96 overflow-y-auto space-y-2">
			{#each filtered as pipeline (pipeline.oid)}
				<div class="section-wrapper !shadow-none !border p-3 flex items-center justify-between">
					<div class="min-w-0">
						<p class="font-bold truncate">{pipeline.name}</p>
						<p class="text-xs dimmed truncate">{pipeline.description}</p>
					</div>
					<button class="button-neutral !px-3" on:click={() => selected = [...selected, pipeline]}>
						<Fa icon={faPlus} />
					</button>
				</div>
			{/each}
		</div>
	</div>

	<hr class="hr" />

	<div class="space-y-2">
		<h4 class="h4">Selected</h4>
		{#each selected as pipeline, i (pipeline.oid)}
			<div class="section-wrapper !shadow-none !border p-3 flex items-center justify-between">
				<p class="font-bold truncate">{i + 1}. {pipeline.name}</p>
				<button class="button-neutral !px-3" on:click={() => selected = selected.filter((p) => p.oid !== pipeline.oid)}>
					<Fa icon={faTrash} />
				</button>
			</div>
		{/each}
	</div>
</div>
```

```svelte
<!-- DUUIWeb/src/routes/processes/ProcessConfigurationDrawer.svelte (new; follow ProcessDrawer/ComponentDrawer header pattern) -->
<script lang="ts">
	import { getDrawerStore, getToastStore } from '@skeletonlabs/skeleton'
	import { faClose, faSave, faEdit } from '@fortawesome/free-solid-svg-icons'
	import Fa from 'svelte-fa'
	import Number from '$lib/svelte/components/Input/Number.svelte'
	import type { DUUIPipeline } from '$lib/duui/pipeline'
	import type { ProcessConfiguration } from '$lib/duui/processConfiguration'
	import { componentDrawerSettings } from '$lib/config'

	const drawerStore = getDrawerStore()
	const toastStore = getToastStore()

	export let pipeline: DUUIPipeline = $drawerStore.meta.pipeline
	export let processId: string = $drawerStore.meta.processId
	export let config: ProcessConfiguration = $drawerStore.meta.config

	const save = async () => {
		// Applies only to *process creation* (and restart params).
		// Existing flow already sends worker_count inside the ProcessSettings POST body: `/api/processes`.
		drawerStore.close()
	}

	const editComponent = (component: DUUIPipeline['components'][number]) => {
		// Reuse the existing ComponentDrawer to update the component globally for its pipeline.
		// That drawer already persists via `PUT /api/components` (see ComponentDrawer.svelte).
		drawerStore.open({
			id: 'component',
			...componentDrawerSettings,
			meta: { component, inEditor: false, example: false }
		})
	}
</script>

<div class="space-y-4 h-full bg-surface-100-800-token">
	<div class="flex items-stretch gap-4 justify-between border-b border-color bg-surface-50-900-token">
		<button class="button-menu border-r border-color" on:click={save}><Fa icon={faSave} /><span>Save</span></button>
		<p class="text-lg self-center">Process config</p>
		<button class="button-menu border-l border-color" on:click={drawerStore.close}><Fa icon={faClose} /><span>Close</span></button>
	</div>

	<div class="p-4 space-y-6">
		<Number min={1} max={100} name="workerCount" label="Worker count" bind:value={config.worker_count} />

		<hr class="hr" />

		{#each pipeline.components as component (component.oid)}
			<div class="section-wrapper p-4 space-y-2">
				<div class="flex items-center justify-between gap-4">
					<p class="font-bold">{component.name}</p>
					<button class="button-neutral" on:click={() => editComponent(component)}>
						<Fa icon={faEdit} /><span>Edit</span>
					</button>
				</div>
				<!-- Scale etc is edited globally in ComponentDrawer (ComponentOptions.svelte already has scale). -->
			</div>
		{/each}
	</div>
</div>
```

```ts
// NOTE: no new "process component configuration" endpoint needed if you want scale/etc to change
// globally for that component (i.e. update the component itself).
//
// Existing persistence path:
// - `PUT /api/components` -> backend `PUT /components/:id` updates the component doc (incl. options.scale)
// - used in `DUUIWeb/src/lib/svelte/components/Drawer/ComponentDrawer.svelte`
```

```ts
// Scheduling option consistent with current app state (session-only store, not URL-based)
// - Start multi-run in `DUUIWeb/src/routes/processes/+page.svelte` by pushing a queue to `multiProcessQueueStore`
// - Continue in `DUUIWeb/src/routes/processes/[oid]/+page.svelte` when `process.is_finished === true`
```

```ts
// DUUIWeb/src/routes/processes/[oid]/+page.svelte (hook into existing polling update)
import { get } from 'svelte/store'
import { multiProcessQueueStore } from '$lib/store'
import type { ProcessSettings } from '$lib/duui/process'
import type { ProcessConfiguration } from '$lib/duui/processConfiguration'

const createProcessFromQueue = async (settings: ProcessSettings, config: ProcessConfiguration, pipelineId: string) => {
	settings.pipeline_id = pipelineId
	settings.settings.worker_count = config.worker_count
	const res = await fetch('/api/processes', { method: 'POST', body: JSON.stringify(settings) })
	const next = await res.json()
	return next.oid as string
}

async function maybeContinue() {
	const q = get(multiProcessQueueStore)
	if (!q) return
	if (q.index >= q.pipelineIds.length) return multiProcessQueueStore.set(null)
	const nextPipelineId = q.pipelineIds[q.index]
	multiProcessQueueStore.set({ ...q, index: q.index + 1 })
	const nextOid = await createProcessFromQueue(q.baseSettings, q.baseConfiguration, nextPipelineId)
	await goto(`/processes/${nextOid}`)
}

// call once when the polling loop sees `process.is_finished === true`
```
