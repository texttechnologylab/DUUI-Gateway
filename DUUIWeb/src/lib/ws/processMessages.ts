import type { DUUIProcess } from '$lib/duui/process'
import type { DUUIDocument } from '$lib/duui/io'
import type { EventMessage, PayloadKind } from './eventTypes'

export type UpdateMeta = {
	runKey: string
	eventId: number
	timestamp: number
}

export type DUUIStateSnapshot = {
	status: string
	thread: string
	last_updated_at: number
	last_event_id: number
	status_durations: Record<string, number>
}

export type ProcessPatch = Partial<
	Pick<
		DUUIProcess,
		| 'status'
		| 'error'
		| 'progress'
		| 'started_at'
		| 'finished_at'
		| 'is_finished'
		| 'is_terminal'
		| 'initial'
		| 'skipped'
		| 'count'
	>
> & {
	runKey?: string
	total?: number
	thread?: string
	last_updated_at?: number
	last_event_id?: number
	status_durations?: Record<string, number>
}

export type ProcessUpdate = {
	kind: 'ProcessUpdate'
	meta: UpdateMeta
	process: ProcessPatch
}

export type WorkerUpsert = {
	kind: 'WorkerUpsert'
	meta: UpdateMeta
	workerName: string
	worker: DUUIStateSnapshot & { name: string; activeWorkers: number }
}

export type DriverUpsert = {
	kind: 'DriverUpsert'
	meta: UpdateMeta
	driverName: string
	driver: DUUIStateSnapshot & { driverName: string; activeInstances: number }
}

export type InstanceSnapshot = DUUIStateSnapshot & {
	instanceId: string
	endpoint: string
	errorCount: number
	lastErrorPayload: { kind: PayloadKind; content: string }
	lastErrorAt: number
}

export type ComponentUpsert = {
	kind: 'ComponentUpsert'
	meta: UpdateMeta
	componentId: string
	component: DUUIStateSnapshot & {
		componentId: string
		componentName: string
		driverName: string
		activeInstances: number
		instances: Record<string, InstanceSnapshot>
	}
}

export type InstanceUpsert = {
	kind: 'InstanceUpsert'
	meta: UpdateMeta
	componentId: string
	instanceId: string
	instance: InstanceSnapshot
}

export type DocumentUpsert = {
	kind: 'DocumentUpsert'
	meta: UpdateMeta
	documentKey: string
	document: DUUIDocument
}

export type ProcessUpdateItem =
	| ProcessUpdate
	| WorkerUpsert
	| DriverUpsert
	| ComponentUpsert
	| InstanceUpsert
	| DocumentUpsert

export type UpdateMessage = {
	kind: 'update'
	process_id: string
	updates: ProcessUpdateItem[]
}

export type InitMessage = {
	kind: 'init'
	process_id: string
	process: DUUIProcess
	documents: DUUIDocument[]
	server_time: number
}

export type ProcessMessage = InitMessage | EventMessage | UpdateMessage

