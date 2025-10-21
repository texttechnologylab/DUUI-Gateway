<!--
	@component
	A modal component that can be used to make the user confirm an action like deleting something.
-->
<script lang="ts">
	import { faClose } from '@fortawesome/free-solid-svg-icons'
	import { getModalStore, getToastStore } from '@skeletonlabs/skeleton'
	import Fa from 'svelte-fa'
	import CodeInput from '$lib/svelte/components/Input/CodeInput.svelte'
	import Link from '$lib/svelte/components/Link.svelte'
	import { infoToast } from '$lib/duui/utils/ui'

	const toastStore = getToastStore()
	const modalStore = getModalStore()

	export let title: string = $modalStore[0].meta['title'] || ''
	export let email: string = $modalStore[0].meta['email'] || ''
	export let userId: string = $modalStore[0].meta['userId'] || ''
	export let message: string = $modalStore[0].meta['message'] || ''
	export let endpoint: string = $modalStore[0].meta['endpoint'] || ''

	let hasError = false

	const verifyCode = async (code: string) => {
		const verifyCode = await fetch(`/auth/${endpoint}`, {
			method: 'POST',
			body: JSON.stringify({ userId: userId, code })
		})

		hasError = !verifyCode.ok

		// toastStore.trigger(infoToast("Code sent for verification: " + (hasError ? "Failed" : "Success")))

		if (!verifyCode.ok) {
			const codeMessage = (await verifyCode.json()).error

			if ($modalStore[0].response) {
				$modalStore[0]?.response(false)
				// modalStore.close()
			}
		} else {
			if ($modalStore[0].response) {
				$modalStore[0]?.response(true)

				modalStore.close()
			}
		}
	}
</script>

<div class="z-50 bg-surface-50-900-token w-modal rounded-md overflow-hidden border border-color">
	<div class="modal-header bg-surface-100-800-token">
		<h3 class="h3">{title}</h3>
		<button on:click={modalStore.close}>
			<Fa icon={faClose} size="lg" />
		</button>
	</div>
	<div class="modal-body">
		<div class="p-8 space-y-8">
			<p>{message}</p>
			<Link href={email} />
			<CodeInput error={hasError} minutes={10} on:verify={(e) => verifyCode(e.detail.code)} />

			<!--			<div class="modal-footer">-->
<!--				<div class="flex-center-4 justify-start">-->
<!--					<button-->
<!--						class="button-neutral button-modal hover:!variant-filled-error"-->
<!--						on:click={() => {-->
<!--							if ($modalStore[0].response) {-->
<!--								$modalStore[0]?.response(true)-->
<!--								modalStore.close()-->
<!--							}-->
<!--						}}-->
<!--					>-->
<!--						<span>{textYes}</span>-->
<!--					</button>-->
<!--					<button-->
<!--						class="button-primary button-modal"-->
<!--						on:click={() => {-->
<!--							if ($modalStore[0].response) {-->
<!--								$modalStore[0]?.response(false)-->
<!--								modalStore.close()-->
<!--							}-->
<!--						}}-->
<!--					>-->
<!--						<span>{textNo}</span>-->
<!--					</button>-->
<!--				</div>-->
<!--			</div>-->
		</div>
	</div>

	<!-- <div class="space-y-8">
		
		<div class="p-4 px-8 border-t border-color grid grid-cols-2 items-center gap-4 justify-end"> -->
</div>
