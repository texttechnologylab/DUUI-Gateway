<!--
	@component
	A component that represent a DUUIComponent in its compact form.
-->

<script lang="ts">
	import DriverIcon from '$lib/svelte/components/DriverIcon.svelte'
	import AnnotationChip from '$lib/svelte/components/AnnotationChip.svelte'

	import pkg from 'lodash'
	const { cloneDeep } = pkg

	import { type DUUIComponent } from '$lib/duui/component'
	import { slugify } from '$lib/duui/utils/text'
	import { faClone, faEdit } from '@fortawesome/free-solid-svg-icons'
	import { getDrawerStore, type DrawerSettings } from '@skeletonlabs/skeleton'
	import { createEventDispatcher } from 'svelte'
	import Fa from 'svelte-fa'
	import { componentDrawerSettings } from '$lib/config'

	export let component: DUUIComponent
	export let example: boolean = false
	export let inEditor: boolean = false
	export let cloneable: boolean = false
	export let editable: boolean = true

	const dispatcher = createEventDispatcher()

	const drawerStore = getDrawerStore()

	const openDrawer = () => {
		const drawer: DrawerSettings = {
			id: 'component',
			...componentDrawerSettings,
			meta: { component, inEditor, example }
		}
		drawerStore.open(drawer)
	}
</script>

<li
	id={slugify(component.name)}
	class="section-wrapper scroll-mt-4 md:scroll-mt-24 pointer-events-auto
	{!component.driver || !component.name || !component.target ? '!border-error-500' : ''}
	"
>
	<AnnotationChip
		connector="input"
		values={component.inputs}
		componentId={component.id}
	/>
	<header
		class="flex justify-between gap-4 items-center p-4 bg-surface-50-900-token dark:bg-surface-200-700-token"
	>
		<div class="md:flex md:items-center grid gap-4">
			<DriverIcon driver={component.driver} />
			<p class="md:h4 grow">{component.name}</p>
		</div>
		<div class="scroll-mt-4 md:scroll-mt-16 flex-col-reverse gap-4 md:flex-row flex">
			{#if cloneable}
				<button
					class="pointer-events-auto button-neutral !border-none"
					on:click={() => {
						dispatcher('clone', { component: cloneDeep(component) })
					}}
				>
					<Fa icon={faClone} size="lg" />
					<p>Clone</p>
				</button>
			{/if}
			<button
				disabled={!editable}
				class="pointer-events-auto button-neutral !border-none"
				on:click={openDrawer}
			>
				<Fa icon={faEdit} size="lg" />
				<p>Edit</p>
			</button>
		</div>
	</header>
	<AnnotationChip
		connector="output"
		values={component.outputs}
		componentId={component.id}
	/>
</li>
