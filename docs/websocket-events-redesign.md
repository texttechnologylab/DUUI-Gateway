# WebSocket/Event Redesign Sketch (DUUI Gateway)

Goal: make the frontend derive **all live process state** (process status, progress, document updates, etc.) from the **event stream** as events arrive.

This document sketches the changes needed across backend → websocket → frontend types → dispatcher → stores.

---

## 0) TypeScript types (copy/paste block)

All TS types live here at the top so the rest of the doc can reference them by name.

```ts
// Serialized model as sent by the gateway today (matches DUUIEventController serializers).

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

	// Context as sent by backend serializer: includes `kind` (SimpleName).
	// Mirrors `DUUIEventController.serialize(DUUIContext)` (structured union contexts, flat leaf contexts).

	export type ComposerContext = {
		kind: 'ComposerContext'
		event_id: string
		status: string
		thread: string
		payload?: payload
		runKey: string
		pipeline_status: Record<String, String>
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
```

## 1) Current backend event shape (as serialized today)

Produced by `DUUIEventController.insertMany(...)`.

```ts
// websocket payload per message (MongoDB doc and WS broadcast are the same shape)
type WsEventEnvelope = {
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
```

Important: backend `serialize(DUUIContext)` now emits `context.kind` and also embeds `event_id/status/thread/payload` inside the context.

---

## 2) Target design

### 2.1 Frontend types mirror backend types 1:1

First step: copy the **new DUUI context types and hierarchy** into TypeScript. Deserialization is intentionally minimal:

```ts
const msg = JSON.parse(raw) as WsEventEnvelope
```

No runtime validators in the first iteration; the dispatcher will use presence/shape checks.

### 2.2 Dispatcher updates stores based on event.context subtype(s)

Frontend maintains a set of stores representing the latest known state per entity:

Stores (Svelte `writable` or equivalent):

1. `composerContextStore` (single)
2. `driverContextStores: Map<driverName, DriverContext>` (key is `DriverContext.driver`)
3. `workerContextStores: Map<threadName, WorkerContext>` (keyed by `context.thread`)
4. `documentContextStores: Map<documentKey, DocumentContext>`
   - key choice: **document path** for now (available in serialized context as `path`)
   - optional improvement: add `document_id` to backend serialization if needed
5. `componentContextStores: Map<componentUuid, ComponentContext>`
6. `componentInstanceContextStores: Map<instance_id, InstantiatedComponentContext>`
7. `readerContextStore` (single)

Union contexts update multiple stores:

- `DocumentProcessContext`: update document + composer stores
- `DocumentComponentProcessContext`: update document + (instantiated) component store
- `WorkerContext`: update worker store + (optionally) composer store because it contains `composer`

---

## 3) Context as sent via WebSocket (important notes)

- `event.context.kind` exists and is the simple class name (e.g. `DocumentProcessContext`).
- Union contexts are nested objects (e.g. `DocumentProcessContext.document` + `.composer`).
- `event_id/status/thread/payload` are on the context (`event.context.event_id`, `event.context.status`, `event.context.thread`, `event.context.payload`).

---

## 4) Dispatcher logic (sketch)

### 4.1 Dispatch by `context.kind`

No inference needed; use `switch (msg.event.context.kind)`.

### 4.2 Update strategy

Each store update is a **shallow merge** where incoming fields overwrite previous fields.

Rules:
- Always trust newest event by arrival order (or by `timestamp` if needed).
- For maps, create entry if missing.
- For union contexts: update both/all relevant stores.

Pseudo:

```ts
function dispatchEvent(msg: WsEventEnvelope) {
  // Concrete implementation is in section 4.3.
  // Key idea: update per-entity `writable<Context>` entries (stored inside stable Maps).
}
```

Concrete dispatch/store code is in section 4.3.

### 4.3 Svelte stores + dispatcher sketch (Map of per-entity writables)

Principle: the *map itself* is stable; each entity has its own `writable<Context>`. Updating a single entity store triggers only subscribers of that entity.

```ts
import { writable, type Writable, get } from 'svelte/store'

export const composerContextStore: Writable<ComposerContext | null> = writable(null)
export const readerContextStore: Writable<ReaderContext | null> = writable(null)

export const driverContextStores = new Map<string, Writable<DriverContext>>()
export const workerContextStores = new Map<string, Writable<WorkerContext>>()
export const documentContextStores = new Map<string, Writable<DocumentContext>>()
export const componentContextStores = new Map<string, Writable<ComponentContext>>()
export const componentInstanceContextStores = new Map<string, Writable<InstantiatedComponentContext>>()

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
			// @ts-expect-error sketch: index access
			if ((prev as any)[k] !== (next as any)[k]) {
				store.set(next)
				return
			}
		}
		return
	}
	store.set(next)
}

export function dispatchEvent(msg: WsEventEnvelope) {
	dispatchContext(msg.event, msg.event.context as DUUIContext)
}

export function dispatchContext(event: WsEventEnvelope['event'], ctx: DUUIContext): void {
	switch (ctx.kind) {
		case 'ComposerContext':
			composerContextStore.set(ctx)
			return

		case 'DriverContext':
			update(driverContextStores, ctx.driver, ctx)
			return

		case 'WorkerContext':
			update(workerContextStores, ctx.thread, ctx)
			dispatchContext(event, ctx.composer)
			return

		case 'DocumentContext':
			update(documentContextStores, ctx.path, ctx)
			return

		case 'ComponentContext':
			update(componentContextStores, ctx.component, ctx)
			return

		case 'InstantiatedComponentContext':
			update(componentInstanceContextStores, ctx.instance_id, ctx)
			dispatchContext(event, ctx.component)
			return

		case 'DocumentProcessContext':
			dispatchContext(event, ctx.document)
			dispatchContext(event, ctx.composer)
			return

		case 'DocumentComponentProcessContext':
			dispatchContext(event, ctx.document)
			dispatchContext(event, ctx.instantiated_component)
			return

		case 'ReaderContext':
			readerContextStore.set(ctx)
			return
	}
}
```

---

## 5) Backend changes (to enable/clean up the frontend)

### 5.1 `context.kind` is now the primary routing signal

Backend already sends `context.kind` (simple class name), so frontend dispatch no longer needs inference.

### 5.2 Consider adding `document_id` to DocumentContext serialization

Frontend key for documents is currently `path`.

If a stable DB id is required, backend must include it in `serialize(DocumentContext)` (or add a dedicated `document_id` in context for WS only).

---

## 6) Frontend integration points to update

- WebSocket message parsing currently mixes `process_state`, `documents_delta`, and `event` styles (`DUUIWeb/src/lib/ws/processEvents.ts`).
- Target: treat `WsEventEnvelope` as the **single source of truth** for live updates.
  - Either deprecate `process_state`/`documents_delta`, or implement them as *derived* events in the frontend (optional).
- Views (process page, charts) should read from the new stores instead of directly mutating process/doc state from ad-hoc message types.

---

## 7) Open questions / decisions

1. Document store key: `path` vs stable id inside `document` snapshot?
2. Worker store key: `context.thread` vs explicit worker id inside `WorkerContext`?
3. Should payload be stored per thread/component, or kept on the event stream only?
