import type { Writable } from 'svelte/store'
import { writable, get } from 'svelte/store'

export type PayloadKind =
	| 'STACKTRACE'
	| 'LUA'
	| 'TYPESYSTEM'
	| 'RESPONSE'
	| 'LOGS'
	| 'GENERIC'
	| 'NONE'

export type payload = {
	content: string
	type: PayloadKind
}

export type ComposerContext = {
	kind: 'ComposerContext'
	event_id: string
	status: string
	thread: string
	payload?: payload
	runKey: string
	pipeline_status: Record<string, string>
	progress: number
	total: number
}

export type DriverContext = {
	kind: 'DriverContext'
	event_id: string
	status: string
	thread: string
	payload?: payload
	driver: string
}

export type WorkerContext = {
	kind: 'WorkerContext'
	event_id: string
	status: string
	thread: string
	payload?: payload
	composer: ComposerContext
	name: string
	activeWorkers: number
}

export type DocumentContext = {
	kind: 'DocumentContext'
	event_id: string
	status: string
	thread: string
	payload?: payload
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

export type DocumentProcessContext = {
	kind: 'DocumentProcessContext'
	event_id: string
	status: string
	thread: string
	payload?: payload
	document: DocumentContext
	composer: ComposerContext
}

export type ComponentContext = {
	kind: 'ComponentContext'
	event_id: string
	status: string
	thread: string
	payload?: payload
	component: string
	name: string
	driver: string
	instance_ids: string[]
}

export type InstantiatedComponentContext = {
	kind: 'InstantiatedComponentContext'
	event_id: string
	status: string
	thread: string
	payload?: payload
	component: ComponentContext
	instance_id: string
	endpoint: string
}

export type DocumentComponentProcessContext = {
	kind: 'DocumentComponentProcessContext'
	event_id: string
	status: string
	thread: string
	payload?: payload
	document: DocumentProcessContext
	instantiated_component: InstantiatedComponentContext
}

export type ReaderContext = {
	kind: 'ReaderContext'
	event_id: string
	status: string
	thread: string
	payload?: payload
	total: number
	skipped: number
	read: number
	remaining: number
	used_bytes: number
	total_bytes: number
}

export type DUUIContext =
	| ComposerContext
	| DriverContext
	| WorkerContext
	| DocumentContext
	| DocumentProcessContext
	| ComponentContext
	| InstantiatedComponentContext
	| DocumentComponentProcessContext
	| ReaderContext

export type WsEventEnvelope = {
	timestamp: number
	event: {
		_id: string
		process_id: string
		sender: string | null
		message: string
		level: string
		context: DUUIContext
	}
}

export type ContextStores = {
	composerContextStore: Writable<ComposerContext | null>
	driverContextStores: Map<string, Writable<DriverContext>>
	workerContextStores: Map<string, Writable<WorkerContext>>
	documentContextStores: Map<string, Writable<DocumentContext>>
	componentContextStores: Map<string, Writable<ComponentContext>>
	componentInstanceContextStores: Map<string, Writable<InstantiatedComponentContext>>
	readerContextStore: Writable<ReaderContext | null>
}

export function createEventDispatcher(stores: ContextStores) {
	function update<K, T>(map: Map<K, Writable<T>>, key: K, next: T): void {
		const existing = map.get(key)
		const store = existing ?? writable(next)
		if (!existing) map.set(key, store)

		const prev = get(store)
		if (prev === next) return
		if (
			prev &&
			next &&
			typeof prev === 'object' &&
			typeof next === 'object' &&
			Object.keys(prev as object).length === Object.keys(next as object).length
		) {
			for (const k of Object.keys(prev as object)) {
				// eslint-disable-next-line @typescript-eslint/no-explicit-any
				if ((prev as any)[k] !== (next as any)[k]) {
					store.set(next)
					return
				}
			}
			return
		}
		store.set(next)
	}

	function dispatchContext(ctx: DUUIContext): void {
		switch (ctx.kind) {
			case 'ComposerContext':
				stores.composerContextStore.set(ctx)
				return

			case 'DriverContext':
				update(stores.driverContextStores, ctx.driver, ctx)
				return

			case 'WorkerContext':
				update(stores.workerContextStores, ctx.thread, ctx)
				dispatchContext(ctx.composer)
				return

			case 'DocumentContext':
				update(stores.documentContextStores, ctx.path, ctx)
				return

			case 'ComponentContext':
				update(stores.componentContextStores, ctx.component, ctx)
				return

			case 'InstantiatedComponentContext':
				update(stores.componentInstanceContextStores, ctx.instance_id, ctx)
				dispatchContext(ctx.component)
				return

			case 'DocumentProcessContext':
				dispatchContext(ctx.document)
				dispatchContext(ctx.composer)
				return

			case 'DocumentComponentProcessContext':
				dispatchContext(ctx.document)
				dispatchContext(ctx.instantiated_component)
				return

			case 'ReaderContext':
				stores.readerContextStore.set(ctx)
				return
		}
	}

	return {
		dispatchEvent(message: WsEventEnvelope) {
			dispatchContext(message.event.context)
		}
	}
}

