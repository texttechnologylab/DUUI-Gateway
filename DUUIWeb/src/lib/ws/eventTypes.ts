export type PayloadKind =
	| 'STACKTRACE'
	| 'LUA'
	| 'TYPESYSTEM'
	| 'RESPONSE'
	| 'LOGS'
	| 'GENERIC'
	| 'METRIC_MILLIS'
	| 'NONE'

export type Payload = {
	content: string
	type: PayloadKind
}

export type BaseContext = {
	kind: string
	event_id: string
	status: string
	thread: string
	payload?: Payload
}

export type DefaultContext = BaseContext & { kind: 'DefaultContext' }

export type ComposerContext = BaseContext & {
	kind: 'ComposerContext'
	runKey: string
	pipeline_status: Record<string, string>
	documentCount: number
}

export type WorkerContext = BaseContext & {
	kind: 'WorkerContext'
	name: string
	activeWorkers: number
	composer: ComposerContext
}

export type DriverContext = BaseContext & {
	kind: 'DriverContext'
	driver: string
}

export type DocumentContext = BaseContext & {
	kind: 'DocumentContext'
	path: string
	name: string
	size: number
	progress: number
	error: string | null
	is_finished: boolean
	duration_decode: number
	duration_deserialize: number
	duration_wait: number
	duration_process: number
	started_at: number
	finished_at: number
}

export type DocumentProcessContext = BaseContext & {
	kind: 'DocumentProcessContext'
	document: DocumentContext
	composer: ComposerContext
}

export type ComponentContext = BaseContext & {
	kind: 'ComponentContext'
	component: string
	name: string
	driver: string
	instance_ids: string[]
}

export type InstantiatedComponentContext = BaseContext & {
	kind: 'InstantiatedComponentContext'
	component: ComponentContext
	instance_id: string
	endpoint: string
}

export type DocumentComponentProcessContext = BaseContext & {
	kind: 'DocumentComponentProcessContext'
	document: DocumentProcessContext
	instantiated_component: InstantiatedComponentContext
}

export type ReaderContext = BaseContext & {
	kind: 'ReaderContext'
	total: number
	skipped: number
	read: number
	remaining: number
	used_bytes: number
	total_bytes: number
}

export type ReaderDocumentContext = BaseContext & {
	kind: 'ReaderDocumentContext'
	reader: ReaderContext
	document: DocumentContext
}

export type DUUIContext =
	| DefaultContext
	| ComposerContext
	| WorkerContext
	| DriverContext
	| DocumentContext
	| DocumentProcessContext
	| ComponentContext
	| InstantiatedComponentContext
	| DocumentComponentProcessContext
	| ReaderContext
	| ReaderDocumentContext

export type StoredEvent = {
	_id: string
	process_id: string
	scope_keys: string[]
	sender: string | null
	message: string
	level: string
	context: DUUIContext
}

export type TimelineEntry = {
	timestamp: number
	event: StoredEvent
}

export type TimelineResponse = {
	timeline: TimelineEntry[]
}

export type EventMessage = {
	kind: 'event'
	process_id: string
	timestamp: number
	event: StoredEvent
}

