<!--	
	@component
	A component used to edit the options for a DUUIComponent.
-->
<script lang="ts">
	import { DUUIDockerDriver, DUUIKubernetesDriver, DUUIPodmanDriver, DUUIRemoteDriver, DUUISwarmDriver, type DUUIComponent } from '$lib/duui/component'
	import { equals } from '$lib/duui/utils/text'
	import { SlideToggle } from '@skeletonlabs/skeleton'
	import Number from './Number.svelte'
	import JsonDropdownInput from './JsonDropdownInput.svelte'
	export let component: DUUIComponent

	let labels: string[] = []
	component.options.labels = component.options.labels ? component.options.labels : []

	// Initialize chosenLabels from existing labels and ensure reactivity by
	// assigning a new Map instance when updated.
	let chosenLabels: Map<string, string> = new Map(component.options.labels.map(l => [l, l]))

	// Use $effect but read `component.driver` synchronously so it's tracked as
	// a dependency. The fetch is performed inside an async IIFE so that any
	// awaited reads don't interfere with dependency tracking.
	$effect(() => {
		const drv = component.driver

		;(async () => {
			const response = await fetch(`/api/users/labels/?driver=${drv}`, {
				method: 'GET'
			})

			if (response.ok) {
				const result = await response.json()
				labels = result["labels"] ?? []

				if (!labels.length) {
					component.options.labels = []
				} else {
					labels = [...labels]
					component.options.labels = component.options.labels.filter((label) => labels.includes(label))
				}

				// Recreate the Map to trigger Svelte reactivity
				chosenLabels = new Map(component.options.labels.map(label => [label, label]))
			}
		})()
	})

</script>

<div class="gap-4 flex flex-col items-start justify-center">
	{#if equals(component.driver, DUUIDockerDriver)}

		<div class="space-y-1">
			<label for="slider" class="flex items-center">
				<SlideToggle
					background="bg-surface-100-800-token"
					active="bg-primary-500"
					rounded="rounded-full"
					border="bordered-soft"
					name="slider"
					bind:checked={component.options.docker_image_fetching}
				>
					<span class="form-label">Docker Image Fetching</span>
				</SlideToggle>
			</label>
			<p>When checked, attempts to download the image if it is doesn't exist locally.</p>
		</div>
	{/if}

	{#if equals(component.driver, DUUIRemoteDriver)}
		<div class="space-y-1">
			<label for="slider" class="flex items-center">
				<SlideToggle
					background="bg-surface-200-700-token"
					active="bg-primary-500"
					rounded="rounded-full"
					name="slider"
					bind:checked={component.options.ignore_200_error}
				>
					<span class="form-label">Ignore 200 errors</span>
				</SlideToggle>
			</label>
			<p>When checked, ignores all errors as long as a status code of 200 is returned.</p>
		</div>
	{/if}
	<div class="space-y-2">
		<Number min={1} max={20} name="scale" label="Scale" bind:value={component.options.scale} />
		<p>Components can create multiple replicas of themselves for distributed processing.</p>
	</div>
	
	{#if equals(component.driver, DUUIDockerDriver)
		|| equals(component.driver, DUUIKubernetesDriver)
		|| equals(component.driver, DUUISwarmDriver)
		|| equals(component.driver, DUUIPodmanDriver)
	}
		{#key component.driver}
			{#if labels && labels.length > 0}
				<div class="space-y-2 ">
					<h4 class="h4">Labels</h4>
					<JsonDropdownInput bind:dropdownList={labels} bind:target={component.options.labels} bind:data={chosenLabels}/>
				</div>
			{/if}
		{/key}
	{/if}
	<!-- <TextInput name="host" label="Host" bind:value={component.options.host} /> -->
	<!-- <div class="space-y-4">
		<TextInput name="host" label="Username" bind:value={component.options.registry_auth.username} />
		<TextInput name="host" label="Password" bind:value={component.options.registry_auth.password} />
	</div> -->
</div>
