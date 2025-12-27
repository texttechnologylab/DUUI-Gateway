import { writable, type Writable } from 'svelte/store'
import { get } from 'svelte/store'
import type { DUUIComponent } from '$lib/duui/component'
import type { ProcessSettings } from '$lib/duui/process'

export type PipelineProcessConfigurationDraft = {
	pipelineId: string
	worker_count: number
	components: DUUIComponent[]
}

export type MultiProcessQueue = {
	pipelineIds: string[]
	index: number
	baseSettings: ProcessSettings
	configByPipelineId: Record<string, PipelineProcessConfigurationDraft | undefined>
}

export const multiProcessQueueStore: Writable<MultiProcessQueue | null> = writable(null)

export const cloneProcessSettings = (settings: ProcessSettings): ProcessSettings =>
	JSON.parse(JSON.stringify(settings)) as ProcessSettings

export const applyPipelineDraftIfAny = async (
	pipelineId: string,
	fetchFn: typeof fetch = fetch
) => {
	const q = get(multiProcessQueueStore)
	const draft = q?.configByPipelineId?.[pipelineId]
	if (!draft) return

	const responses = await Promise.all(
		(draft.components ?? []).map((component) =>
			fetchFn('/api/components', { method: 'PUT', body: JSON.stringify(component) })
		)
	)

	const firstError = responses.find((r) => !r.ok)
	if (firstError) throw new Error(await firstError.text())
}

export const buildPayloadForPipeline = (
	pipelineId: string,
	fallbackSettings: ProcessSettings
): ProcessSettings => {
	const q = get(multiProcessQueueStore)
	const draft = q?.configByPipelineId?.[pipelineId]
	const payload = cloneProcessSettings(q?.baseSettings ?? fallbackSettings)
	payload.pipeline_id = pipelineId
	payload.settings.worker_count = draft?.worker_count ?? payload.settings.worker_count
	return payload
}

export const maybeContinueMultiQueue = async (
	fetchFn: typeof fetch = fetch,
	fallbackSettings?: ProcessSettings
): Promise<string | null> => {
	const q = get(multiProcessQueueStore)
	if (!q) return null

	if (q.index >= q.pipelineIds.length) {
		multiProcessQueueStore.set(null)
		return null
	}

	const nextPipelineId = q.pipelineIds[q.index]
	multiProcessQueueStore.set({ ...q, index: q.index + 1 })

	await applyPipelineDraftIfAny(nextPipelineId, fetchFn)

	const payload = buildPayloadForPipeline(nextPipelineId, fallbackSettings ?? q.baseSettings)

	const res = await fetchFn('/api/processes', { method: 'POST', body: JSON.stringify(payload) })
	if (!res.ok) throw new Error(await res.text())

	const next = await res.json()
	return next.oid as string
}
