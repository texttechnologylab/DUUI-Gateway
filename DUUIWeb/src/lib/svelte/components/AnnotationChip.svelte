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

	const inputContainerClass =
		'relative flex flex-wrap gap-3 px-2 pb-1 m-4 bg-surface-100-800-token rounded-md p-2'
	const outputContainerClass =
		'relative flex flex-wrap items-end justify-end gap-3 px-2 pb-1 m-4 bg-surface-100-800-token rounded-md p-2'

	const inputChipClass =
		'uima-chip-input relative inline-flex items-center px-4 py-2 pr-8 variant-soft-primary text-sm rounded-md'
	const outputChipClass =
		'uima-chip-output relative inline-flex items-center px-4 py-2 pr-8 variant-soft-primary text-sm rounded-md'
</script>

<div
	class={values.length === 0
		? 'hidden'
		: connector === 'input'
		? inputContainerClass
		: outputContainerClass}
>
	<span
		class={connector === 'input'
			? 'absolute -top-3 left-3 bg-surface-100-800-token px-2 text-xs rounded-md text-surface-400-900-token font-medium'
			: 'absolute -top-3 right-3 bg-surface-100-800-token px-2 text-xs rounded-md text-surface-400-900-token font-medium'}
	>
		{connector === 'input' ? 'Input' : 'Output'}
	</span>
	{#each values as value (value)}
		{@const parts = splitClass(value)}
		<div
			class={connector === 'input' ? inputChipClass : outputChipClass}
			data-connector={connector}
			data-component-id={componentId}
			data-type={value}
		>
			<div class="relative inline-block pb-3">
				<span class="block text-[0.9rem]">{parts.simple}</span>
				{#if parts.pkg}
					<span
						class="absolute left-0 block w-full text-[0.5rem] truncate" title={parts.pkg}
					>
						{parts.pkg}
					</span>
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
