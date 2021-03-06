let followingUsers = []

function getCurrentViewButton () {
  const currViewId = document.getElementsByClassName('current-view')[0].id
  return document.getElementById(currViewId + '-btn')
}

function applyViewButtonStyle () {
  getCurrentViewButton()?.classList.toggle('opacity-50')
  getCurrentViewButton()?.classList.toggle('pointer-events-none')
  getCurrentViewButton()?.classList.toggle('font-medium')
}

async function restoreToken () {
  const token = localStorage.getItem('winsome_token')
  const username = localStorage.getItem('winsome_username')

  if (token && username) {
    try {
      await onLoginDone(username, token)
    } catch {
      localStorage.removeItem('winsome_token')
      localStorage.removeItem('winsome_username')
    }
  }
}

function getMyUsername () {
  return document.getElementById('my-username').innerHTML
}

async function onLoginDone (username, token) {
  document.getElementById('my-username').innerHTML = username
  axios.defaults.headers.common['Authorization'] = 'Bearer ' + token
  await getUsers()
  await getMyPosts()
  await getFeed()
  await getWallet()
  switchViewTo('feed-view')
  document.getElementById('navbar').classList.remove('hidden')
}

async function login () {
  try {
    const response = await axios.post('login', getLoginParameters())
    const token = response.data.split('\n')[0]
    console.log(token)
    const username = getLoginParameters().split('\n')[0]
    notify('Login effettuato con successo')
    localStorage.setItem('winsome_token', token)
    localStorage.setItem('winsome_username', username)
    await onLoginDone(username, token)
  } catch {
    showErrorNotification('Username o password errati.')
  }
}

async function logout () {
  try {
    await axios.post('logout', getMyUsername())
    axios.defaults.headers.common['Authorization'] = undefined
    localStorage.removeItem('winsome_token')
    localStorage.removeItem('winsome_username')
    switchViewTo('login-view')
    notify('Logout effettuato con successo')
    document.getElementById('my-posts-content').innerHTML = ''
    document.getElementById('feed-content').innerHTML = ''
    document.getElementById('user-list-content').innerHTML = ''
    document.getElementById('navbar').classList.add('hidden')
  } catch {
    showErrorNotification('Si ?? verificato un errore durante il logout.')
  }
}

async function getUsers () {
  try {
    const response = await axios.get('users')
    const followingList = await axios.get('users/following')
    followingUsers = followingList.data
    document.getElementById('user-list-content').innerHTML = getUserListHtml(
      response.data
    )
    console.log(response.data)
  } catch {
    showErrorNotification(
      'Si ?? verificato un errore accedendo alla lista degli utenti. Riprova.'
    )
  }
}

async function getWallet () {
  const response = await axios.get('wallet')
  console.log('wallet', response)
  document.getElementById('transactions').innerHTML = getTransactionsHtml(
    response.data.transactions
  )
  document.getElementById('wallet-balance').innerHTML =
    Math.round(response.data.balance * 100) / 100
}

async function getMyPosts () {
  const response = await axios.get('posts/my-posts')
  if (response.data.length == 0) {
    document.getElementById('my-posts-content').innerHTML =
      'Non hai post. Clicca sul bottone in alto a sinistra per crearne uno!'
  }
  response.data.forEach(post => {
    console.log(post)
    document.getElementById('my-posts-content').innerHTML += getPostHtml(post)
  })
}

async function getFeed () {
  document.getElementById('feed-content').innerHTML = ''
  const response = await axios.get('posts')
  console.log('response', response)
  if (response.data.length == 0) {
    document.getElementById('feed-content').innerHTML =
      'Non ci sono post nel tuo feed. Inizia a seguire qualcuno!'
  }
  response.data.forEach(post => {
    document.getElementById('feed-content').innerHTML += getPostHtml(post)
  })
}

async function createPost () {
  try {
    const response = await axios.post('posts', {
      title: document.getElementById('new-post-title').value,
      content: document.getElementById('new-post-content').value
    })
    const myPostsContent = document.getElementById('my-posts-content')
    myPostsContent.innerHTML =
      getPostHtml(response.data) + myPostsContent.innerHTML
    notify('Post creato con successo')
    console.log(response)
    togglePostCreation()
  } catch {
    showErrorNotification(
      'Si ?? verificato un errore durante la creazione del post. Riprova.'
    )
  }
}

function _followUser (username) {
  followUser(username).then(() => {})
  document.getElementById(
    'user-' + username + '-follow-btn'
  ).innerHTML = `<button onclick="_unfollowUser('${username}')" class="ml-auto rounded-md px-4 py-0.5 text-sm bg-red-500 text-white hover:bg-red-600">Smetti di seguire</button>`
}
function _unfollowUser (username) {
  unfollowUser(username).then(() => {})
  document.getElementById(
    'user-' + username + '-follow-btn'
  ).innerHTML = `<button onclick="_followUser('${username}')" class="ml-auto rounded-md px-4 py-0.5 text-sm bg-green-500 text-white hover:bg-green-600">Segui</button>`
}

function _createComment (postId) {
  createComment(postId).then(() => {})
}

function _vote (postId, value) {
  vote(postId, value).then(() => {})
}

function _rewin (postId) {
  rewin(postId).then(() => {})
}

function _deletePost (postId) {
  if (!confirm('Sei sicuro di voler cancellare questo post?')) {
    return
  }
  deletePost(postId).then(() => {})
}

async function followUser (username) {
  try {
    await axios.put('users/following', username)
    await getFeed()
    notify('Hai iniziato a seguire ' + username)
  } catch {
    showErrorNotification('Si ?? verificato un errore. Riprova.')
  }
}

async function unfollowUser (username) {
  try {
    await axios.delete('users/following', { data: username })
    await getFeed()
    notify('Hai smesso di seguire ' + username)
  } catch {
    showErrorNotification('Si ?? verificato un errore. Riprova.')
  }
}

async function createComment (postId) {
  console.log('creating')
  const content = document.getElementById(`post-${postId}-comment-input`).value
  try {
    const response = await axios.post(`posts/${postId}/comments`, {
      content
    })
    console.log(response)
    const commentsElement = document.getElementById(
      `post-${postId}-comments-content`
    )
    commentsElement.innerHTML += getCommentsHtml([response.data])
    document.getElementById(`post-${postId}-comment-input`).value = ''
  } catch {
    showErrorNotification('Si ?? verificato un errore. Riprova.')
  }
}

async function rewin (postId) {
  try {
    const response = await axios.post(`posts/${postId}/rewin`)
    const myPostsContent = document.getElementById('my-posts-content')
    myPostsContent.innerHTML =
      getPostHtml(response.data) + myPostsContent.innerHTML
    notify('Post rewinnato con successo. Lo trovi nel tuo blog!')
  } catch {
    showErrorNotification('Si ?? verificato un errore. Riprova.')
  }
}

async function deletePost (postId) {
  try {
    await axios.delete(`posts/${postId}`)
    document.getElementById(`post-${postId}`).outerHTML = ''
    notify('Post cancellato con successo.')
  } catch {
    showErrorNotification('Si ?? verificato un errore. Riprova.')
  }
}

async function vote (postId, value) {
  try {
    await axios.post(`posts/${postId}/rate`, value)
    document
      .getElementById(`post-${postId}-${value == 1 ? 'up' : 'down'}vote-btn`)
      .classList.add(
        'pointer-events-none',
        `bg-${value == 1 ? 'green' : 'red'}-200`
      )
    document
      .querySelector(`#post-${postId}-reactions`)
      .querySelector(value == 1 ? '.upvotes' : '.downvotes')
      .classList.remove('hidden')
    document
      .querySelector(`#post-${postId}-reactions`)
      .querySelector(
        value == 1 ? '.upvotes-content' : '.downvotes-content'
      ).innerHTML += getMyUsername()
  } catch (e) {
    showErrorNotification('Si ?? verificato un errore. Riprova.')
    throw e
  }
}

function getLoginParameters () {
  return `${document.getElementById('form-username').value}\n${
    document.getElementById('form-password').value
  }`
}

function showErrorNotification (msg) {
  alert(msg)
}

function switchViewTo (viewId) {
  applyViewButtonStyle()
  document
    .getElementsByClassName('current-view')[0]
    .classList.remove('current-view')
  document.getElementById(viewId).classList.add('current-view')
  applyViewButtonStyle()
}

function notify (msg) {
  document.getElementById('notification').classList.remove('opacity-0')
  document.getElementById('notification').classList.add('opacity-100')
  document.getElementById('notification').innerHTML = msg
  setTimeout(() => {
    document.getElementById('notification').classList.remove('opacity-100')
    document.getElementById('notification').classList.add('opacity-0')
  }, 3000)
}

function showComments (postId) {
  document
    .getElementById('post-' + postId + '-comments')
    .classList.toggle('hidden')
  const toggle = document.getElementById('post-' + postId + '-comments-toggle')
  toggle.innerHTML =
    toggle.innerHTML[0] == 'M' ? 'Nascondi commenti' : 'Mostra commenti'
}

function getPostHtml (post) {
  const actualPost = post.originalPost ?? post
  let ret = `<div id="post-${
    post.id
  }" class="post relative w-full p-6 mx-auto my-4 border border-gray-300 rounded-md post hover:shadow-inner">
  <button class="${
    post.author != getMyUsername() ? 'hidden pointer-events-none' : ''
  } absolute opacity-60 hover:opacity-100 right-0 top-0 mt-4 mr-4 bg-red-500 w-6 h-6 rounded-full text-white font-bold pb-1" onclick="_deletePost('${
    post.id
  }')">&times;</button>
  ${
    post.originalPost
      ? `<div class="mb-6">
    <div class="flex space-x-2">
    <div class="w-10 h-10 rounded-full shadow-md avatar bg-gray-50"></div>
    <h3 class="my-auto text-xl font-medium text-blue-800">Rewinnato da ${post.author}</h3>
    </div>
  </div>`
      : ''
  }
  <div class="${
    post.originalPost ? 'rounded-md shadow-md border my-2 p-4 bg-blue-50' : ''
  }">
                  <div class="flex mb-4 space-x-2">
                  <div class="w-10 h-10 rounded-full shadow-md avatar bg-gray-50"></div>
                <div class="flex space-x-2">
                  <h3 class="my-auto text-xl font-medium text-blue-800">${
                    actualPost.title
                  }</h3>
                  <p class="my-auto text-sm text-gray-400 font-light">&mdash; di ${
                    actualPost.author
                  }</p>
                </div>
                </div>
                    <div class="flex">
                        <div class="w-11/12">
                            <p>${actualPost.content}</p>
                        </div>
                        <div class="ml-auto flex flex-col space-y-6">
                            <button id="post-${post.id}-upvote-btn" class="${
    post.reactions.some(r => r.username == getMyUsername() && r.value == 1)
      ? 'pointer-events-none bg-green-200'
      : ''
  } ${
    post.author == getMyUsername() ? 'hidden' : ''
  } p-1 px-2 rounded-xl transition-colors duration-75 text-green-800 hover:text-green-900 hover:bg-green-200 active:bg-green-300 font-semibold" onclick="_vote('${
    post.id
  }', 1)">+1</button>
                            <button id="post-${post.id}-downvote-btn" class="${
    post.reactions.some(r => r.username == getMyUsername() && r.value == -1)
      ? 'pointer-events-none bg-red-200'
      : ''
  } ${
    post.author == getMyUsername() ? 'hidden' : ''
  } p-1 px-2 rounded-xl transition-colors duration-75 text-red-800 hover:text-red-900 hover:bg-red-200 active:bg-red-300 font-semibold" onclick="_vote('${
    post.id
  }', -1)">&minus;1</button>
  <button id="post-${post.id}-rewin-btn" class="${
    actualPost.author != getMyUsername() && !post.originalPost
      ? ''
      : 'hidden pointer-events-none'
  } text-sm py-1.5 px-2 rounded-xl transition-colors duration-75 text-indigo-800 hover:text-indigo-900 hover:bg-indigo-200 active:bg-indigo-300 font-semibold" onclick="_rewin('${
    post.id
  }', -1)">Rewin</button>
                        </div>
                    </div>
                    <div class="mt-4 text-xs flex space-x-6" id="post-${
                      post.id
                    }-reactions">${getReactionsHtml(post.reactions)}</div></div>
                    <p id="post-${
                      post.id
                    }-comments-toggle" onclick="showComments('${
    post.id
  }')" class="mt-6 mb-4 text-blue-900 cursor-pointer hover:underline">Mostra commenti</p>
                    <div id="post-${post.id}-comments" class="hidden">
                        <div id="post-${
                          post.id
                        }-comments-content">${getCommentsHtml(
    post.comments
  )}</div>
                        <div class="flex space-x-2">
                            <input id="post-${
                              post.id
                            }-comment-input" type="text" placeholder="Commenta..." class="rounded-full py-2 px-3 border border-gray-300 w-4/5" />
                            <button class="bg-blue-800 hover:bg-blue-900 rounded-xl shadow-md py-1 px-3 my-auto text-white" onclick="_createComment('${
                              post.id
                            }')">Invia</button>
                        </div>
                    </div>
                </div>`
  return ret
}

function getReactionsHtml (reactions) {
  const upVotesHtml = `<div class="my-2"><p><strong class="upvotes text-green-800 mr-1 ${
    reactions.some(r => r.value == 1) ? '' : 'hidden'
  }">+1</strong><span class="upvotes-content">${reactions
    .filter(r => r.value == 1)
    .reduce((acc, reaction) => (acc += reaction.username + ', '), '')
    .slice(0, -2)}</span></p></div>`

  const downVotesHtml = `<div class="my-2"><p><strong class="downvotes text-red-800 mr-1 ${
    reactions.some(r => r.value == -1) ? '' : 'hidden'
  }">&minus;1</strong><span class="downvotes-content">${reactions
    .filter(r => r.value == -1)
    .reduce((acc, reaction) => (acc += reaction.username + ', '), '')
    .slice(0, -2)}</span></p></div>`

  return upVotesHtml + downVotesHtml
}

function getTransactionsHtml (transactions) {
  if (transactions.length == 0) {
    return '<p>Non ci sono ancora transazioni.</p>'
  }
  return transactions.reduce(
    (acc, transaction) =>
      (acc += `
  <div class="py-2.5 px-2 mb-2 flex bg-gray-200 rounded-lg">
  <div class="mr-auto flex items-center"><div class="h-4 w-4 rounded-full bg-green-600 mr-3"></div>${(transaction.delta >
  0
    ? '+'
    : '') +
    Math.round(transaction.delta * 100) / 100}</div>
  <div>${new Date(transaction.timestamp).toLocaleDateString('it-IT', {
    month: 'short',
    day: 'numeric',
    year: 'numeric'
  })}, alle ${new Date(transaction.timestamp).toLocaleTimeString('it-IT')}</div>
  </div>`),
    ''
  )
}

function getCommentsHtml (comments) {
  return comments.reduce(
    (acc, comment) =>
      (acc += `<div class="my-0.5">
                            <div class="flex mb-6 space-x-2">
                                <div class="flex space-x-1">
                                    <div class="w-4 h-4 my-auto rounded-full shadow-md avatar bg-gray-50"></div>
                                    <p class="text-blue-800 font-medium">${comment.authorUsername}</p>
                                </div>
                                <p class="my-auto">${comment.content}</p>
                            </div>
                        </div>`),
    ''
  )
}

function getUserListHtml (userList) {
  return userList.reduce(
    (acc, user) =>
      (acc += `<div class="flex my-3">
                  <div class="w-4 h-4 my-auto rounded-full shadow-md avatar bg-gray-50"></div>
                  <p class="ml-1 mr-4 text-blue-800 font-medium">${
                    user.username
                  }</p>
                  <div class="flex space-x-2">
                    ${getTagsHtml(user.tags)}
                  </div>
                  <div class="ml-auto" id="user-${
                    user.username
                  }-follow-btn"><button onclick="_${
        followingUsers.includes(user.username) ? 'un' : ''
      }followUser('${user.username}')" class="${
        user.username == getMyUsername() ? 'hidden' : ''
      } rounded-md px-4 py-0.5 text-sm ${
        followingUsers.includes(user.username)
          ? 'bg-red-500 hover:bg-red-600'
          : 'bg-green-500 hover:bg-green-600'
      } text-white">${
        followingUsers.includes(user.username) ? 'Smetti di seguire' : 'Segui'
      }</button></div>
               </div>`),
    ''
  )
}

function getTagsHtml (tagList) {
  return tagList.reduce((acc, tag) => {
    const { bg, text } = getRandomTagColor()
    acc += `<div style="min-width: 75px" class="text-center px-2 py-0.5 rounded-full shadow-sm opacity-90 text-sm text-${text} bg-${bg}">${tag}</div>`
    return acc
  }, '')
}

function getRandomTagColor () {
  const colors = ['yellow', 'green', 'blue', 'indigo', 'purple', 'red']
  const color = colors[Math.round(Math.random() * (colors.length - 1))]
  return {
    bg: color + '-300',
    text: color + '-900'
  }
}

function togglePostCreation () {
  document.getElementById('post-creation-backdrop').classList.toggle('hidden')
  document.getElementById('post-creation-modal').classList.toggle('hidden')
}
