<script lang="ts">
	import { splitClass } from '$lib/duui/component'
	import { successToast } from '$lib/duui/utils/ui'
	import { faCopy } from '@fortawesome/free-solid-svg-icons'
	import { clipboard, getToastStore } from '@skeletonlabs/skeleton'
	import Fa from 'svelte-fa'

	export let values: string[] = []
	export let connector: 'input' | 'output' = 'input'
	export let componentId: string

	const toastStore = getToastStore()

	const inputContainerClass = 'flex flex-wrap gap-3 px-2 pb-1 m-4'
	const outputContainerClass =
		'flex flex-wrap items-end justify-end gap-3 px-2 pb-1 m-4'

	const inputChipClass =
		'uima-chip-input relative inline-flex items-center px-4 py-2 pr-8 variant-soft-primary border text-sm rounded-md'
	const outputChipClass =
		'uima-chip-output relative inline-flex items-center px-4 py-2 pr-8 variant-soft-primary border text-sm rounded-md'
</script>

<div
	class={values.length === 0
		? 'hidden'
		: connector === 'input'
		? inputContainerClass
		: outputContainerClass}
>
	{#each values as value (value)}
		{@const parts = splitClass(value)}
		<div
			class={connector === 'input' ? inputChipClass : outputChipClass}
			data-connector={connector}
			data-component-id={componentId}
			data-type={value}
		>
			<div class="flex flex-col items-start">
				<span class="text-sm font-semibold">{parts.simple}</span>
				{#if parts.pkg}
					<span class="text-[0.7rem]">{parts.pkg}</span>
				{/if}
			</div>
			<button
				type="button"
				class="absolute top-1 right-1 text-xs opacity-80 hover:opacity-100 transition-opacity"
				use:clipboard={value}
				on:click={() =>
					toastStore.trigger(successToast('Copied annotation to clipboard!'))
				}
			>
				<Fa icon={faCopy} />
			</button>
		</div>
	{/each}
</div>
