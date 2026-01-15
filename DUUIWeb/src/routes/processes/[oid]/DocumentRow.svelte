<script lang="ts">
	export type DocumentRowState = {
		path: string
		name: string
		progress: number
		status: string
		size: number
		started_at: number
		finished_at: number
	}
	import Fa from 'svelte-fa'
	import { Status } from '$lib/duui/monitor.js'
	import { equals, formatFileSize } from '$lib/duui/utils/text'
	import { formatMilliseconds } from '$lib/duui/utils/time'
	import { getDocumentStatusIcon } from '$lib/duui/utils/ui'

	export let state: DocumentRowState
	export let maxProgress: number
	export let onClick: () => void

	$: name = state?.name ?? ''
	$: progress = state?.progress ?? 0
	$: status = state?.status ?? Status.Unknown
	$: size = state?.size ?? 0
	$: duration = Math.max(0, (state?.finished_at ?? 0) - (state?.started_at ?? 0))
</script>

<button
	class="rounded-none
		grid grid-cols-3 lg:grid-cols-5 gap-8 items-center
		p-4
		hover:variant-filled-primary
		text-xs lg:text-sm text-start"
	on:click={onClick}
>
	<p class="truncate" title={name}>{name}</p>
	<div class="md:flex items-center justify-start md:gap-4 text-start">
		<p>{Math.round((Math.min(progress, maxProgress) / maxProgress) * 100)} %</p>
	</div>
	<p class="flex justify-start items-center gap-2 md:gap-4">
		<Fa
			icon={getDocumentStatusIcon({ ...document, status })}
			size="lg"
			class="{equals(status, Status.Active)
				? 'animate-spin-slow'
				: equals(status, Status.Waiting)
				? 'animate-hourglass'
				: ''} w-6"
		/>
		<span>{status}</span>
	</p>
	<p class="hidden lg:inline-flex">{formatFileSize(size)}</p>
	<p class="hidden lg:inline-flex">{formatMilliseconds(duration)}</p>
</button>
