<!--
	@component
	Input for UIMA annotation class names using chips.
	The chip shows the simple class name prominently and the package in smaller, low-emphasis text.
-->
<script lang="ts">
	import { splitClass, type UIMAAnnotationClass } from '$lib/duui/component'
	import { successToast } from '$lib/duui/utils/ui'
	import { faClose, faCopy } from '@fortawesome/free-solid-svg-icons'
	import { clipboard, getToastStore } from '@skeletonlabs/skeleton'
	import { createEventDispatcher } from 'svelte'
	import Fa from 'svelte-fa'

	const toastStore = getToastStore()
	const dispatcher = createEventDispatcher()

	export let label: string
	export let values: UIMAAnnotationClass[] = []
	export let placeholder: string = 'Add UIMA annotation class and press enter...'
	export let disabled: boolean = false

	let current = ''

	const normalize = (value: string): UIMAAnnotationClass =>
		value.trim() as UIMAAnnotationClass

	const push = () => {
		const raw = current.trim()
		if (!raw) return

		const value = normalize(raw)
		if (!values.includes(value)) {
			values = [...values, value]
		}

		dispatcher('push', { text: value })
		current = ''
	}

	const discard = (value: UIMAAnnotationClass) => {
		values = values.filter((v) => v !== value)
		dispatcher('discard', { text: value })
	}

</script>

<div class="group space-y-2">
	<div class="label flex flex-col">
		<span class="form-label">{label}</span>

		<div
			class="input-wrapper !p-0 flex flex-col gap-3 {values.length > 0
				? '!pb-2'
				: ''}"
		>
			<input
				class="{values.length > 0
					? 'ring-0'
					: ''} border-none appearance-none ring-0 bg-transparent focus:ring-0 py-3 px-3 w-full"
				type="text"
				bind:value={current}
				{placeholder}
				{disabled}
				on:keypress={(event) => {
					if (event.key === 'Enter') {
						event.preventDefault()
						push()
					}
				}}
			/>

			<div class={values.length === 0 ? 'hidden' : 'flex flex-wrap gap-3 px-2 pb-1'}>
				{#each values as value (value)}
					{@const parts = splitClass(value)}
					<div class="inline-flex items-center px-4 py-2 variant-soft-primary border text-sm rounded-md">
						<div class="flex flex-col items-start">
							<span class="text-sm font-semibold">{parts.simple}</span>
							{#if parts.pkg}
								<span class="text-[0.7rem]	">{parts.pkg}</span>
							{/if}
						</div>
						<div class="flex flex-col gap-1">
							<button
								type="button"
								class="ml-3 text-xs opacity-80 hover:opacity-100 transition-opacity"
								use:clipboard={value}
								on:click={() => toastStore.trigger(successToast("Copied annotation to clipboard!"))}
							>
								<Fa icon={faCopy} />
							</button>
							<button
								type="button"
								class="ml-3 text-xs opacity-80 hover:opacity-100 transition-opacity"
								on:click={() => discard(value)}
							>
								<Fa icon={faClose} />
							</button>

						</div>
					</div>
				{/each}
			</div>
		</div>
	</div>
</div>
