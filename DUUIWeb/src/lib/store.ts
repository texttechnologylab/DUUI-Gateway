import { localStorageStore } from '@skeletonlabs/skeleton'
import { writable, type Writable } from 'svelte/store'
import { blankComponent, type DUUIComponent } from './duui/component'
import { blankPipeline, type DUUIPipeline } from './duui/pipeline'
import { blankSettings, type ProcessSettings } from './duui/process'

// Stores the current state of the pipelines that is edited and used the brower's local storage.
export const currentPipelineStore: Writable<DUUIPipeline> = localStorageStore(
	'currentPipelineStore',
	blankPipeline()
)

export const examplePipelineStore: Writable<DUUIPipeline> = writable(blankPipeline())

// Stores the current state of the process settings.
export const processSettingsStore: Writable<ProcessSettings> = writable(blankSettings())

// Used in the documentation for illustration purposes.
export const exampleComponent: Writable<DUUIComponent> = writable(blankComponent('', 0))

// Store that tracks dark mode. Used for charts in both chart.ts files to check for dark mode.
export const isDarkModeStore: Writable<boolean> = writable(false)

// Store for the logged in User.
export const userSession: Writable<User> = writable(undefined)

export const labelStore: Writable<DUUILabels | undefined> = writable(undefined)

export const groupStore: Writable<DUUIGroups | undefined> = writable(undefined)