import type { DUUIProcess } from '$lib/duui/process'
import { get, writable, type Writable } from 'svelte/store'
import type { DUUIDocument } from '$lib/duui/io'
import type { InitMessage, ProcessMessage, ProcessUpdateItem, UpdateMessage } from './processMessages'

export type LiveProcessStore = {
	process: Writable<DUUIProcess>
	documentsByPath: Map<string, Writable<DUUIDocument>>
	documents: Writable<DUUIDocument[]>
	apply: (message: ProcessMessage) => void
}

function upsertStore<K, T>(
	map: Map<K, Writable<T>>,
	key: K,
	next: T,
	merge?: (prev: T, next: T) => T
): Writable<T> {
	const existing = map.get(key)
	if (!existing) {
		const store = writable(next)
		map.set(key, store)
		return store
	}

	if (!merge) {
		existing.set(next)
		return existing
	}

	const prev = get(existing)
	existing.set(merge(prev, next))
	return existing
}

function mergeDocument(prev: DUUIDocument, next: DUUIDocument): DUUIDocument {
	return { ...prev, ...next }
}

function applyUpdateItem(store: LiveProcessStore, item: ProcessUpdateItem): void {
	switch (item.kind) {
		case 'ProcessUpdate': {
			store.process.update((prev) => ({ ...prev, ...item.process }))
			return
		}

		case 'DocumentUpsert': {
			upsertStore(store.documentsByPath, item.documentKey, item.document, mergeDocument)
			store.documents.update((prev) => {
				const next = [...prev]
				const index = next.findIndex((d) => d.path === item.documentKey)
				if (index >= 0) next[index] = { ...next[index], ...item.document }
				else next.push(item.document)
				return next
			})
			return
		}

		default:
			return
	}
}

function applyInit(store: LiveProcessStore, msg: InitMessage): void {
	store.process.set(msg.process)
	store.documents.set(msg.documents)
	for (const doc of msg.documents) upsertStore(store.documentsByPath, doc.path, doc, mergeDocument)
}

function applyUpdate(store: LiveProcessStore, msg: UpdateMessage): void {
	for (const u of msg.updates) {
		applyUpdateItem(store, u)
	}
}

export function createLiveProcessStore(initial: DUUIProcess): LiveProcessStore {
	const store: LiveProcessStore = {
		process: writable(initial),
		documentsByPath: new Map(),
		documents: writable([]),
		apply: (message: ProcessMessage) => {
			switch (message.kind) {
				case 'init':
					applyInit(store, message)
					return
				case 'update':
					applyUpdate(store, message)
					return
				case 'event':
					return
			}
		}
	}
	return store
}
