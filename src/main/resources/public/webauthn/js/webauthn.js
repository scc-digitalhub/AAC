
async function requestOptions(providerId, username) {
    console.log("fetch options for " + providerId + " user " + username);
    const response = await fetch(`/auth/webauthn/assertionOptions/${providerId}`,
        {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(
                {
                    "username": username
                }
            ),
        }
    );

    if (!response.ok) {
        throw Error(response.statusText);
    }

    return response.json();
}



async function webauthnAuthentication(form) {
    const providerId = form.elements.provider.value;
    const username = form.elements.username.value;
    if (!username) {
        return;
    }

    const msg = document.getElementById("webauthn_error_" + providerId);
    if (msg) {
        msg.style = "display:none;";
    }

    try {
        //fetch options and key
        const assertionRequestAndKey = await requestOptions(providerId, username);
        console.log(assertionRequestAndKey);

        const { loginKey, assertionRequest } = assertionRequestAndKey;


        //build credentials options
        const credentialGetOptions = {
            publicKey: {
                ...assertionRequest.publicKey,
                allowCredentials: assertionRequest.publicKey.allowCredentials
                    && assertionRequest.publicKey.allowCredentials.map(credential => ({
                        ...credential,
                        id: base64URLStringToBuffer(credential.id),
                    })),

                challenge: base64URLStringToBuffer(assertionRequest.publicKey.challenge),

                // Warning: Extension inputs could also contain binary data that needs encoding
                extensions: assertionRequest.publicKey.extensions,
            },
        }

        console.log(credentialGetOptions)

        //fetch credentials from browser    
        const publicKeyCredential = await navigator.credentials.get(credentialGetOptions)
        const authenticatorReponse = {
            type: publicKeyCredential.type,
            id: publicKeyCredential.id,
            response: {
                authenticatorData: bufferToBase64URLString(publicKeyCredential.response.authenticatorData),
                clientDataJSON: bufferToBase64URLString(publicKeyCredential.response.clientDataJSON),
                signature: bufferToBase64URLString(publicKeyCredential.response.signature),
                userHandle: publicKeyCredential.response.userHandle && bufferToBase64URLString(publicKeyCredential.response.userHandle),
            },
            // Warning: Client extension results could also contain binary data that needs encoding
            clientExtensionResults: publicKeyCredential.getClientExtensionResults(),
        }

        const finishLoginResp = await fetch(
            `/auth/webauthn/assertions/${providerId}`,
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(
                    {
                        "key": loginKey,
                        "assertion": authenticatorReponse
                    }
                ),
            }
        );

        if (finishLoginResp.status == 200) {
            alert(await finishLoginResp.text());
            window.location.href = "/index.html"
        } else {
            alert(finishLoginResp.status, await finishLoginResp.text())
        }
    } catch (e) {
        console.log(e);
        if (msg) {
            msg.innerHTML = e;
            msg.style = "display: block;";
        }
    }

    console.log("end");
}



function base64URLStringToBuffer(base64URLString) {//https://git.io/J1zgG
    const base64 = base64URLString.replace(/-/g, '+').replace(/_/g, '/');
    const padLength = (4 - (base64.length % 4)) % 4;
    const padded = base64.padEnd(base64.length + padLength, '=');
    const binary = window.atob(padded);
    const buffer = new ArrayBuffer(binary.length);
    const bytes = new Uint8Array(buffer);
    for (let i = 0; i < binary.length; i++) {
        bytes[i] = binary.charCodeAt(i);
    }
    return buffer;
}

function bufferToBase64URLString(buffer) {//https://git.io/J1zgb
    const bytes = new Uint8Array(buffer);
    let str = '';
    for (const charCode of bytes) {
        str += String.fromCharCode(charCode);
    }
    const base64String = window.btoa(str);
    return base64String.replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
}


