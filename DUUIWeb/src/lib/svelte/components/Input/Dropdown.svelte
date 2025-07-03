<!--
	@component
	A Dropdown component where one option can be selected.
-->

<script lang="ts">
	import { equals, toTitleCase } from '$lib/duui/utils/text'
	import type { Placement } from '@floating-ui/dom'
	import { faCheck, faChevronDown, faL, type IconDefinition } from '@fortawesome/free-solid-svg-icons'
	import { ListBox, ListBoxItem, popup, type PopupSettings } from '@skeletonlabs/skeleton'
	import Fa from 'svelte-fa'
	import { v4 as uuidv4 } from 'uuid'
	import {onMount} from "svelte";

	export let label: string = ''
	export let name: string = label

	export let capitalize = true

	export let options: string[] | number[] | Map<string | number, string | number> | Record<string, string | number>
	export let value: string | number

	export let initFirst: boolean = false 

	export let icon: IconDefinition = faChevronDown
	export let placement: Placement = 'bottom-end'

	export let offset: number = 4

	export let style: string = 'input-wrapper'
	export let rounded: string = 'rounded-md'
	export let border: string = 'border border-color'
	export let textAlign: string = 'text-start'
	export let minWidth: string = 'md:min-w-[220px]'

	let dropdownId = '';

	onMount(() => {
		dropdownId = uuidv4();
	});

	const dropdown: PopupSettings = {
		event: 'click',
		target: name,
		placement: placement,
		closeQuery: `.listbox-item`,
		middleware: {
			offset: offset
		}
	}

	// Convert options into a Map if necessary.
	let optionsMap: Map<string | number, string | number>
	if (options instanceof Map) {
		optionsMap = options


	} else if (Array.isArray(options)) {
		optionsMap = new Map(
			options.map(item => [item, item] as [string | number, string | number])
		)
	} else if (typeof options === 'object' && options !== null && !Array.isArray(options)) {
		optionsMap = new Map(
			Object.entries(options) as [string, string|number][]
		);
	} else {
		throw new Error("options must be an array or a Map")
	}


	// $: {
	// 	alert("INIT: \n" + JSON.stringify(Object.fromEntries(optionsMap)))
	// }
	$: {
		// console.log(`Size: ${optionsMap.size} | initFirst: ${initFirst} | values: ${JSON.stringify(Object.fromEntries(optionsMap))}`);

		if (initFirst && !value && optionsMap.size > 0) {
			value  = optionsMap.keys().toArray().shift() ?? value;
		}
	}
	$: rawLabel = (optionsMap.get(value) ?? value) as string;
	$: displayLabel = rawLabel
		? (capitalize ? toTitleCase(rawLabel) : rawLabel)
		: '';
</script>
<div class="label flex flex-col {minWidth}">
	{#if label}
		<span class="form-label {textAlign}">{label} </span>
	{/if}
	<button
		class="flex items-center !justify-between gap-2 px-3 py-2 leading-6  {border} {rounded} {style}"
		use:popup={dropdown}
	>
	<span>{displayLabel}</span>
		<Fa {icon} />
	</button>
</div>

<div data-popup={name} id={dropdownId} class="fixed overflow-y-auto h64 mt-1 z-10">
	<div class="popup-solid p-2 {minWidth} overflow-scroll max-h-96">
		<ListBox class="overflow-hidden" rounded="rounded-md" spacing="space-y-2">
			{#each Array.from(optionsMap.entries()) as [key, displayValue]}
				<ListBoxItem
					on:change
					bind:group={value}
					{name}
					value={key}
					rounded="rounded-md"
					hover="hover:bg-surface-100-800-token"
					active="variant-filled-primary"
				>
					<svelte:fragment slot="lead">
						<Fa class={equals('' + value, '' + key) ? '' : 'invisible'} icon={faCheck} />
					</svelte:fragment>
					{displayValue}
				</ListBoxItem>
			{/each}
		</ListBox>
	</div>
</div>
