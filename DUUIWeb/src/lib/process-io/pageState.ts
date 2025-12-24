import type { Writable } from 'svelte/store'

export enum ProcessPagePhase {
	Editing = 'Editing',
	Uploading = 'Uploading',
	DirectoryFetching = 'DirectoryFetching'
}

export enum ProcessPageMode {
	New = 'New',
	Restart = 'Restart'
}

export type ProcessPageState = {
	mode: ProcessPageMode
	phase: ProcessPagePhase
	uploadFilesCount: number
}

export type ProcessPageStateStore = Writable<ProcessPageState>

